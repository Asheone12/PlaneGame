package com.muen.planegame

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.muen.planegame.bomb.BombManager
import com.muen.planegame.bullet.BulletManager
import com.muen.planegame.databinding.ActivityMainBinding
import com.muen.planegame.plane.EnemyPlaneManager
import com.muen.planegame.plane.PlayerPlane
import com.muen.planegame.util.BaseActivity
import com.muen.planegame.view.CrossRocker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class MainActivity : BaseActivity<ActivityMainBinding>(), CoroutineScope by MainScope() {
    private lateinit var map: BackgroundMap
    private lateinit var playerPlane: PlayerPlane

    override fun onCreateViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    override fun onResume() {
        super.onResume()
        hideStatusBar()
    }

    override fun onBackPressed() {
        AppHelper.isRunning = false
        android.os.Process.killProcess(android.os.Process.myPid())
        super.onBackPressed()
    }

    /**
     * 全屏
     */
    private fun hideStatusBar() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            val v = this.window.decorView
            v.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            decorView.systemUiVisibility = uiOptions
        }
    }

    override fun initView() {
        super.initView()
        viewBinding.ibtnBack.setOnClickListener {
            onBackPressed()
        }
        initPauseButton()
        initGamePad() // 方向键
        initAttackButton() // 开火键
        initScoreCount() // 评分和击落数
        initCollision() // 碰撞数
        initFirePower() // 火力升级
        initBombView() // 爆雷
        initSurfaceView()
    }



    private fun initSurfaceView() {
        viewBinding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                AppHelper.isRunning = true
                AppHelper.widthSurface = viewBinding.surfaceView.width
                AppHelper.heightSurface = viewBinding.surfaceView.height
                AppHelper.bound = Rect(0, 0, viewBinding.surfaceView.width, viewBinding.surfaceView.height)

                map = BackgroundMap(this@MainActivity) // 初始化地图
                playerPlane = PlayerPlane.init() // 初始化玩家飞机
                EnemyPlaneManager.init() // 初始化敌方飞机
                BulletManager.init() // 初始化子弹数据
                BombManager.init() // 初始化爆炸管理
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                launch(Dispatchers.IO) {
                    while (AppHelper.isRunning) {
                        val canvas = holder.lockCanvas()
                        // 绘制背景
                        map.draw(canvas, width, height)
                        if (!AppHelper.isPause) {
                            // 绘制子弹
                            BulletManager.getInst().drawPlayerBullet(canvas)
                            BulletManager.getInst().drawBossBullet(canvas)
                            // 绘制我方飞机
                            playerPlane.draw(canvas)
                            // 绘制敌方飞机
                            EnemyPlaneManager.getInst().draw(canvas)
                            // 绘制爆炸
                            BombManager.getInst().drawAll(canvas)
                        }
                        canvas?.let { holder.unlockCanvasAndPost(it) }
                    }
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                AppHelper.isRunning = false
                // 资源释放
                map.relase()
                PlayerPlane.getInst().release()
                BulletManager.getInst().release()
                BombManager.getInst().release()
                EnemyPlaneManager.getInst().release()
            }

        })
    }

    private fun initPauseButton() {
        viewBinding.info.vgInfomation.visibility =View.GONE
        viewBinding.btnPause.setOnClickListener {
            AppHelper.isPause = !AppHelper.isPause
            viewBinding.info.vgInfomation.visibility = if (AppHelper.isPause) View.VISIBLE else View.GONE
        }
        viewBinding.info.btnJianShu.setOnClickListener { openBrowser(viewBinding.info.btnJianShu.text.toString().split("\n")[1]) }
        viewBinding.info.btnQQ.setOnClickListener { openBrowser(viewBinding.info.btnQQ.text.toString().split("\n")[1]) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initAttackButton() {
        viewBinding.btnAttack.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    viewBinding.btnAttack.setBackgroundResource(R.drawable.btn_oval_true)
                    PlayerPlane.getInst().attack(true)
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    viewBinding.btnAttack.setBackgroundResource(R.drawable.btn_oval_false)
                    PlayerPlane.getInst().attack(false)
                }

            }
            true
        }
    }


    /**
     * 碰撞数
     */
    private fun initCollision() {
        LiveEventBus.get(AppHelper.COLLI_EVENT, Int::class.java)
            .observe(this, Observer {
                val oldValue = viewBinding.tvCollision.text.toString().toInt()
                val df = DecimalFormat("00000")
                val newValue = df.format(it + oldValue)
                viewBinding.tvCollision.text = newValue
            })
    }

    private fun initBombView() {
        LiveEventBus.get(AppHelper.BOMB_ADD_EVENT, Int::class.java)
            .observe(this, Observer {
                viewBinding.ratBomb.rating++
            })
        LiveEventBus.get(AppHelper.BOMB_RESET_EVENT, Int::class.java)
            .observe(this, Observer {
                viewBinding.ratBomb.rating = it.toFloat()
            })
        viewBinding.btnBomb.setOnClickListener {
            if (viewBinding.ratBomb.rating > 0) {
                EnemyPlaneManager.getInst().fullScreenBomb()
                viewBinding.ratBomb.rating--
            }
        }
    }

    /**
     * 火力强度
     */
    private fun initFirePower() {
        viewBinding.pbPower.max = 20
        viewBinding.pbPower.progress = 0
        LiveEventBus.get(AppHelper.FIRE_EVENT, Int::class.java)
            .observe(this, Observer {
                viewBinding.tvFirePower.text = when (it) {
                    in (0..10) -> "一级"
                    in (11..20) -> "二级"
                    else -> "三级"
                }

                viewBinding.pbPower.progress = it
            })
    }

    /**
     * 总分数和击落数
     */
    private fun initScoreCount() {
        LiveEventBus.get(AppHelper.SCORE_EVENT, Int::class.java)
            .observe(this) {
                val oldScore = viewBinding.tvScore.text.toString().toInt()
                val df = DecimalFormat("00000")
                val newScore = df.format(it + oldScore)
                viewBinding.tvScore.text = newScore
                val oldCount = viewBinding.tvCount.text.toString().toInt()
                val df2 = DecimalFormat("00000")
                val newCount = df2.format(oldCount + 1)
                viewBinding.tvCount.text = newCount
            }
        LiveEventBus.get(AppHelper.SCORE_EVENT, Int::class.java)
            .post(999)
    }

    /**
     * 初始化游戏十字键
     */
    private fun initGamePad() {
        viewBinding.gamePad.setActionListener { _, direction, evt ->
            onGamePadKey(evt.actionMasked, direction) // 响应方向盘操作
        }
    }

    private var lastDirection = ""
    private fun onGamePadKey(action: Int, direction: String) {
        when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (direction != lastDirection) playerPlane.releaseAction()
                when (direction) {
                    CrossRocker.RIGHT -> playerPlane.actionRight()
                    CrossRocker.LEFT -> playerPlane.actionLeft()
                    CrossRocker.TOP -> playerPlane.actionTop()
                    CrossRocker.BOTTOM -> playerPlane.actionBottom()
                    CrossRocker.TOP_RIGHT -> {
                        playerPlane.actionTop();playerPlane.actionRight()
                    }
                    CrossRocker.BOTTOM_RIGHT -> {
                        playerPlane.actionBottom();playerPlane.actionRight()
                    }
                    CrossRocker.BOTTOM_LEFT -> {
                        playerPlane.actionBottom();playerPlane.actionLeft()
                    }
                    CrossRocker.TOP_LEFT -> {
                        playerPlane.actionTop();playerPlane.actionLeft()
                    }
                }
                lastDirection = direction
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                playerPlane.releaseAction()
            }
        }
    }

    /**
     * 调用第三方浏览器打开
     * @param url 要浏览的资源地址
     */
    private fun openBrowser(url: String?) {
        val intent = Intent()
        intent.action = "android.intent.action.VIEW"
        val content_url = Uri.parse(url)
        intent.data = content_url
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.setClassName("com.android.browser", "com.android.browser.BrowserActivity")
        startActivity(intent)
    }
}