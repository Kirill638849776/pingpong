import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.FloatControl
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.Timer

class PingPong(private val darkMode: Boolean) : JPanel(), ActionListener {
    private val width = 640
    private val height = 480
    private val paddleWidth = 12
    private val paddleHeight = 80
    private val ballSize = 16

    private var leftPaddleY = (height - paddleHeight) / 2
    private var rightPaddleY = (height - paddleHeight) / 2
    private var ballX = width / 2.0
    private var ballY = height / 2.0
    private var ballDX = 4.0
    private var ballDY = 3.0

    private var leftScore = 0
    private var rightScore = 0
    private var upLeft = false
    private var downLeft = false

    private var ballColor = Color.WHITE
    private var leftPaddleColor = Color.CYAN
    private var rightPaddleColor = Color.MAGENTA

    private var threeCount = 0
    private var broken = false

    private var secondsLeftDisplay = 10

    private val timer: Timer
    private val hereFile = File("here.here")
    private val imHereFile = File("imhere.txt")

    init {
        background = if (darkMode) Color(20, 0, 0) else Color(10, 10, 30)
        isFocusable = true
        preferredSize = Dimension(width, height)
        addKeyListener(TAdapter())
        timer = Timer(16, this)
        timer.start()
    }

    fun updateSecondsLeft(s: Int) {
        secondsLeftDisplay = s
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        g.color = leftPaddleColor
        g.fillRect(10, leftPaddleY, paddleWidth, paddleHeight)
        g.color = rightPaddleColor
        g.fillRect(width - 10 - paddleWidth, rightPaddleY, paddleWidth, paddleHeight)
        g.color = ballColor
        g.fillOval(ballX.toInt(), ballY.toInt(), ballSize, ballSize)

        g.color = if (darkMode) Color(200, 0, 0) else Color.WHITE
        g.font = Font("Helvetica", Font.BOLD, 36)
        g.drawString(leftScore.toString(), width / 2 - 60, 50)
        g.drawString(rightScore.toString(), width / 2 + 40, 50)

        g.color = Color.GRAY
        for (y in 0 until height step 20) {
            g.fillRect(width / 2 - 1, y, 2, 10)
        }

        if (darkMode) {
            g.color = Color(255, 60, 60)
            g.font = Font("Consolas", Font.BOLD, 64)
            val text = secondsLeftDisplay.toString()
            val fm = g.fontMetrics
            val tw = fm.stringWidth(text)
            g.drawString(text, width / 2 - tw / 2, height / 2 + 20)
        }
    }

    override fun actionPerformed(e: ActionEvent) {
        if (broken) return
        movePaddles()
        moveBot()
        moveBall()
        repaint()
    }

    private fun movePaddles() {
        val speed = 6
        if (upLeft && leftPaddleY > 0) leftPaddleY -= speed
        if (downLeft && leftPaddleY < height - paddleHeight) leftPaddleY += speed
    }

    private fun moveBot() {
        val botSpeed = 4.5
        val target = ballY - paddleHeight / 2.0
        if (rightPaddleY < target && rightPaddleY < height - paddleHeight) {
            rightPaddleY += botSpeed.toInt().coerceAtLeast(1)
        } else if (rightPaddleY > target && rightPaddleY > 0) {
            rightPaddleY -= botSpeed.toInt().coerceAtLeast(1)
        }
    }

    private fun moveBall() {
        ballX += ballDX
        ballY += ballDY

        if (ballY <= 0 || ballY >= height - ballSize) {
            ballDY = -ballDY
            ballColor = randomColor()
            playSoundAsync(400, 0.06, false)
        }

        if (ballX <= 10 + paddleWidth && ballY + ballSize >= leftPaddleY && ballY <= leftPaddleY + paddleHeight && ballDX < 0) {
            ballDX = -ballDX
            ballX = (10 + paddleWidth).toDouble()
            ballColor = randomColor()
            leftPaddleColor = randomColor()
            playSoundAsync(600, 0.06, false)
        }

        if (ballX + ballSize >= width - 10 - paddleWidth && ballY + ballSize >= rightPaddleY && ballY <= rightPaddleY + paddleHeight && ballDX > 0) {
            ballDX = -ballDX
            ballX = (width - 10 - paddleWidth - ballSize).toDouble()
            ballColor = randomColor()
            rightPaddleColor = randomColor()
            playSoundAsync(600, 0.06, false)
        }

        if (ballX < 0) {
            rightScore++
            resetBall()
        } else if (ballX > width) {
            leftScore++
            resetBall()
        }
    }

    private fun resetBall() {
        ballX = width / 2.0
        ballY = height / 2.0
        ballDX = if (Math.random() > 0.5) 4.0 else -4.0
        ballDY = if (Math.random() > 0.5) 3.0 else -3.0
        ballColor = randomColor()
    }

    private fun randomColor(): Color {
        return Color(
            80 + (Math.random() * 175).toInt(),
            80 + (Math.random() * 175).toInt(),
            80 + (Math.random() * 175).toInt()
        )
    }

    private fun playSoundAsync(freq: Int, duration: Double, scary: Boolean) {
        Thread {
            try {
                val sampleRate = 8000f
                val numSamples = (duration * sampleRate).toInt()
                val buf = ByteArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i / sampleRate
                    val f = if (scary) (80.0 - 40.0 * t) else freq.toDouble()
                    val noise = if (scary) (Math.random() * 20).toInt() else 0
                    buf[i] = ((Math.sin(2.0 * Math.PI * f * t) * 100) + noise).toInt().toByte()
                }
                val format = AudioFormat(sampleRate, 8, 1, true, false)
                val line = AudioSystem.getSourceDataLine(format)
                line.open(format)
                try {
                    val gain = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    gain.value = gain.maximum
                } catch (_: Exception) {}
                line.start()
                line.write(buf, 0, buf.size)
                line.drain()
                line.close()
            } catch (_: Exception) {}
        }.start()
    }

    private fun writeImHereText() {
        val text = """
            im here.
            
            Ты думал, это просто игра?
            Ты нажимал кнопки. Ты смеялся.
            А я смотрел.
            
            Теперь я знаю, где ты.
            Файл here.here — это не ошибка.
            Это отметка. Ты вернёшься.
            
            И когда вернёшься — у тебя будет 10 секунд.
            Успей закрыть окно.
            Или я закрою его сам.
            
            died?
        """.trimIndent()
        try {
            imHereFile.writeText(text)
        } catch (_: Exception) {}
    }

    private fun triggerBreak(parent: JFrame) {
        broken = true
        timer.stop()
        playSoundAsync(0, 1.2, true)
        try {
            hereFile.writeText("im here")
        } catch (_: Exception) {}
        writeImHereText()
        JOptionPane.showMessageDialog(parent, "im here", "ERROR", JOptionPane.ERROR_MESSAGE)
        parent.dispose()
        System.exit(0)
    }

    private inner class TAdapter : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_W -> upLeft = true
                KeyEvent.VK_S -> downLeft = true
                KeyEvent.VK_3 -> {
                    threeCount++
                    if (threeCount >= 3) {
                        val frame = (this@PingPong.topLevelAncestor as? JFrame)
                        if (frame != null) triggerBreak(frame)
                    }
                }
                else -> threeCount = 0
            }
        }

        override fun keyReleased(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_W -> upLeft = false
                KeyEvent.VK_S -> downLeft = false
            }
        }
    }
}

/** Долгий скример: нарастающий шум + вой + низкий гул, 5 секунд, громкость 100%. */
fun playScreamer() {
    Thread {
        try {
            val sampleRate = 16000f
            val duration = 5.0
            val numSamples = (duration * sampleRate).toInt()
            val buf = ByteArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i / sampleRate
                val noiseAmp = 20.0 + 100.0 * (t / duration)
                val noise = (Math.random() * noiseAmp * 2 - noiseAmp).toInt()
                val toneFreq = 1800.0 + 400.0 * Math.sin(2.0 * Math.PI * 0.7 * t)
                val tone = (Math.sin(2.0 * Math.PI * toneFreq * t) * 80).toInt()
                val rumble = (Math.sin(2.0 * Math.PI * 60 * t) * 60).toInt()
                buf[i] = (noise + tone + rumble).coerceIn(-128, 127).toByte()
            }
            val format = AudioFormat(sampleRate, 8, 1, true, false)
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format)
            try {
                val gain = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = gain.maximum
            } catch (_: Exception) {}
            line.start()
            line.write(buf, 0, buf.size)
            line.drain()
            line.close()
        } catch (_: Exception) {}
    }.start()
}

/** Короткий писк с заданной частотой, громкость 100%. */
fun playBeep(freq: Double) {
    Thread {
        try {
            val sampleRate = 8000f
            val numSamples = (0.08 * sampleRate).toInt()
            val buf = ByteArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i / sampleRate
                buf[i] = (Math.sin(2.0 * Math.PI * freq * t) * 100).toInt().toByte()
            }
            val format = AudioFormat(sampleRate, 8, 1, true, false)
            val line = AudioSystem.getSourceDataLine(format)
            line.open(format)
            try {
                val gain = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                gain.value = gain.maximum
            } catch (_: Exception) {}
            line.start()
            line.write(buf, 0, buf.size)
            line.drain()
            line.close()
        } catch (_: Exception) {}
    }.start()
}

fun main() {
    val hereFile = File("here.here")
    val darkMode = hereFile.exists()
    val title = if (darkMode) "im here. died?" else "Ping Pong"

    val frame = JFrame(title)
    frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

    var closedManually = false
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            closedManually = true
            frame.dispose()
            System.exit(0)
        }
    })

    val gamePanel = PingPong(darkMode)
    frame.add(gamePanel)
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.isVisible = true

    if (darkMode) {
        var secondsLeft = 10
        val beepTimer = Timer(1000) {
            secondsLeft--
            gamePanel.updateSecondsLeft(secondsLeft)
            if (secondsLeft > 0) {
                val freq = 600.0 + (10 - secondsLeft) * 100
                playBeep(freq)
            } else {
                if (!closedManually) {
                    val flash = JFrame()
                    flash.isUndecorated = true
                    flash.background = Color.WHITE
                    flash.setSize(Toolkit.getDefaultToolkit().screenSize)
                    flash.isVisible = true
                    playScreamer()
                    Timer(5000) {
                        flash.dispose()
                        try { hereFile.delete() } catch (_: Exception) {}
                        frame.dispose()
                        System.exit(0)
                    }.apply { isRepeats = false; start() }
                }
            }
        }
        beepTimer.start()

        val t = Timer(11000) {
            if (!closedManually) {
                try { hereFile.delete() } catch (_: Exception) {}
                frame.dispose()
                System.exit(0)
            }
        }
        t.isRepeats = false
        t.start()
    }
}