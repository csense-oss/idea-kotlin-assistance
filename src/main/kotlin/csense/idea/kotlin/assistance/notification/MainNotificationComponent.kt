package csense.idea.kotlin.assistance.notification

import com.intellij.notification.*
import com.intellij.openapi.application.*
import com.intellij.openapi.components.*
import com.intellij.openapi.project.*
import csense.kotlin.logger.*


class MainNotificationComponent : ProjectComponent {

    override fun projectOpened() {}

    override fun projectClosed() {}

    override fun initComponent() {
        L.usePrintAsLoggers()
        L.isLoggingAllowed(true)
//        ApplicationManager.getApplication()
//                .invokeLater({
//                    Notifications.Bus.notify(NOTIFICATION_GROUP.value
//                            .createNotification(
//                                    "Testing Personal Plugin",
//                                    "Love kotlin",
//                                    NotificationType.INFORMATION,
//                                    null))
//                }, ModalityState.NON_MODAL)
    }


    override fun disposeComponent() {}

    override fun getComponentName(): String {
        return CUSTOM_NOTIFICATION_COMPONENT
    }

    companion object {
        private const val CUSTOM_NOTIFICATION_COMPONENT =
                "Csense - kotlin assistance"
//        private val NOTIFICATION_GROUP = object :
//                NotNullLazyValue<NotificationGroup>() {
//            override fun compute(): NotificationGroup {
//                return NotificationGroup(
//                        "Motivational message",
//                        NotificationDisplayType.STICKY_BALLOON,
//                        true)
//            }
//        }

//        fun showNotificationMessage(message: String, project: Project) {
//            ApplicationManager.getApplication().invokeLater {
//                val notification: Notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.ERROR)
//                Notifications.Bus.notify(notification, project)
//            }
//        }
//
//        val GROUP_DISPLAY_ID_INFO = NotificationGroup("My notification group",
//                NotificationDisplayType.BALLOON, true)
    }
}
