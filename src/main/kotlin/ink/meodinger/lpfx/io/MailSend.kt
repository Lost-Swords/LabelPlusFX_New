package ink.meodinger.lpfx.io

import ink.meodinger.lpfx.INFO
import ink.meodinger.lpfx.get
import ink.meodinger.lpfx.options.Logger
import ink.meodinger.lpfx.type.LPFXTask
import ink.meodinger.lpfx.util.file.exists

import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.io.File
import java.util.*

// Account owned by Meodinger Wang
// DO NOT USE FOR PRIVATE, I trust you.
private const val REPORT_USER = "labelplusfx_report@163.com"
private const val REPORT_AUTH = "SUWAYUTJSKWQNDOF"
//private const val TARGET_USER =

/**
 * Send a mail to Meodinger
 * @param text Content text
 * @param files Files to send, null values will be ingored
 */
fun sendMailSync(text: String, vararg files: File?) {
    // properties
//    TODO 重写方法
    val targetUser = INFO["errorEmail.address"]
    Logger.info("Ready to send the mail to $targetUser","MailSend")
    val props = Properties()
    props.setProperty("mail.transport.protocol", "smtp")
    props.setProperty("mail.smtp.auth", "true")
    props.setProperty("mail.smtp.host", "smtp.163.com")

    // message
    val message = MimeMessage(Session.getInstance(props))

    message.subject = "LPFX log report - ${System.getProperty("user.name")}"
    message.setFrom(REPORT_USER)
    message.setContent(MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply { setText(text) })
        for (file in files) if (file.exists()) addBodyPart(MimeBodyPart().apply { attachFile(file) })
    })

    message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(targetUser))

    Transport.send(message, REPORT_USER, REPORT_AUTH)
    Logger.info("Sended the mail to $targetUser","MailSend")
}

/**
 * @see sendMailSync
 */
fun sendMail(text: String, vararg files: File?): LPFXTask<Unit> = LPFXTask.createTask { sendMailSync(text, *files) }
