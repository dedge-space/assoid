/*
 * Copyright (C) 2020-present, Chenai Nakam(chenai.nakam@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hobby.wei.c.core

import android.app.{NotificationChannel, NotificationManager}
import android.os.Build
import androidx.core.app.{NotificationCompat, NotificationManagerCompat}
import hobby.wei.c.core.Notifications._

import scala.collection.JavaConverters.seqAsJavaListConverter

/**
  * @author Chenai Nakam(chenai.nakam@gmail.com)
  * @version 1.0, 02/05/2020
  */
trait Notifications extends Ctx.Abs {
  def getApp: AbsApp

  def lightColor: Int

  def onBuildPublicVersion(builder: NotificationCompat.Builder): NotificationCompat.Builder

  val isLockScreenPrivate = true

  protected def obtainNotificationBuilder(tpe: EffectType): NotificationCompat.Builder = {
    ensureNotifyMgrInited()
    val builder = new NotificationCompat.Builder(
      context,
      (tpe: @unchecked) match {
        case Mute       => channelIDMute
        case Sound      => channelIDSound
        case Vibration  => channelIDVibra
        case SoundVibra => channelIDSoundVibra
        case Disabled   => throw new IllegalStateException("The args `Disabled` should NEVER be called on this method.") // Won't come to this case.
      }
    )
      //      .setSmallIcon(smallIcon)
      //      .setContentTitle(contentTitle)
      //      .setContentIntent(buildContentIntent())
      //      .setContentText(contentText)
      //      .setAutoCancel(false).setShowWhen(true).setOngoing(true)
      // 在 Android 8.0（API 级别 26）及更高版本上，通知的重要性由通知目标发布渠道的 importance 决定。用户可以在系统设置中
      // 更改通知渠道的重要性（图 12）。在 Android 7.1（API 级别 25）及更低版本上，各通知的重要性由通知的 priority 决定。
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_MESSAGE)
    //        builder.extend(
    //            NotificationCompat.WearableExtender()
    //                .setDisplayIntent(displayIntent)
    //                .setContentIcon(R.drawable.icon_logo)
    //                .setCustomSizePreset(Notification.WearableExtender.SIZE_MEDIUM)
    //        )
    //    if (!nameSender.isEmpty) {
    //            builder.setStyle(
    //                NotificationCompat.MessagingStyle(
    //                    Person.Builder()
    //                        .setName(nameSender).setImportant(true)
    //                        .build()
    //                )
    //                    .addMessage("text 1", System.currentTimeMillis(), Person.Builder().build())
    //                    .addMessage("text 2", System.currentTimeMillis(), Person.Builder().build())
    //            )
    //    }
    //    senderBmp.fold() {
    //      builder.setLargeIcon
    //    }
    builder //.setNumber(numMgs)
//      .setTicker(contentText)
      .setLights(lightColor, 1000, 3000) // Although set on `channels` already, but only color, not others.
    (tpe: @unchecked) match {
      case Mute       => builder.setDefaults(0)
      case Sound      => builder.setVibrate(Array(0)).setDefaults(NotificationCompat.DEFAULT_SOUND)
      case Vibration  => builder.setVibrate(vibrationPattern).setDefaults(0)
      case SoundVibra => builder.setVibrate(vibrationPattern).setDefaults(NotificationCompat.DEFAULT_SOUND)
    }
    if (isLockScreenPrivate) {
      builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      // https://developer.android.com/training/notify-user/build-notification?hl=zh_cn#lockscreenNotification
      builder.setPublicVersion(
        onBuildPublicVersion(new NotificationCompat.Builder(
          context,
          (tpe: @unchecked) match {
            case Mute       => channelIDMute
            case Sound      => channelIDSound
            case Vibration  => channelIDVibra
            case SoundVibra => channelIDSoundVibra
          }
        )).build()
      )
    }
    builder
  }

  protected def obtainFGroundNotificationBuilder(): NotificationCompat.Builder = {
    ensureNotifyMgrInited()
    new NotificationCompat.Builder(context, channelIDForeground)
//      .setSmallIcon(smallIconForeground)
//      .setContentTitle(contentTitleForeground)
//      .setContentIntent(buildContentIntent())
//      .setContentText(contentTextForeground)
      .setAutoCancel(false)
      .setShowWhen(false)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      // Must be set here, only in the channel, does not work. But the phone must be set to slide unlock at least.
      .setVisibility(NotificationCompat.VISIBILITY_SECRET)
  }

  private def ensureNotifyMgrInited(): Unit = notifyMgr.areNotificationsEnabled() // Perform an arbitrary method.

  private val vibrationPattern    = Array[Long](0, 30, 100, 30)
  private val channelIDMute       = getApp.withPackageNamePrefix("notify_mute")
  private val channelIDSound      = getApp.withPackageNamePrefix("notify_sound")
  private val channelIDVibra      = getApp.withPackageNamePrefix("notify_vibration")
  private val channelIDSoundVibra = getApp.withPackageNamePrefix("notify_sound_vibration")
  private val channelIDForeground = getApp.withPackageNamePrefix("foreground_service")

  // 轻易不要调这一句。否则，如果进程不`kill`，后面再`createNotificationChannel(channelID)`也不起作用。
  // notifyMgr.deleteNotificationChannel(channelID)
  protected lazy val notifyMgr = {
    val mgr = NotificationManagerCompat.from(context)
    mgr.createNotificationChannels(
      Seq(
        buildChannel(Mute),
        buildChannel(Sound),
        buildChannel(Vibration),
        buildChannel(SoundVibra),
        buildForegroundChannel()
      ).asJava
    )
    mgr
  }

  private def buildChannel(tpe: EffectType): NotificationChannel = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = new NotificationChannel(
        (tpe: @unchecked) match {
          case Mute       => channelIDMute
          case Sound      => channelIDSound
          case Vibration  => channelIDVibra
          case SoundVibra => channelIDSoundVibra
          case Disabled   => ??? // Won't come to this case.
        },
        (tpe: @unchecked) match {
          case Mute       => "Mute"
          case Sound      => "Sound"
          case Vibration  => "Vibration"
          case SoundVibra => "Sound Vibration"
          case Disabled   => ??? // Won't come to this case.
        },
        // 如果是 IMPORTANCE_LOW，就会像 toast 一样弹出到屏幕顶部。
        // `紧急/HIGH`才能发出声音。
        tpe match {
          case Mute => NotificationManager.IMPORTANCE_LOW
          case _    => NotificationManager.IMPORTANCE_HIGH
        }
      )
      channel.setShowBadge(true) // 是否显示通知圆点（App 图标右上角的圆点。如果支持的话）
      channel.enableLights(true)
      channel.setLightColor(lightColor)
      channel.setLockscreenVisibility(if (isLockScreenPrivate) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_SECRET)
      (tpe: @unchecked) match {
        case Mute  => channel.enableVibration(false)
        case Sound => channel.enableVibration(false)
        case Vibration =>
          channel.enableVibration(true)
          channel.setVibrationPattern(vibrationPattern)
          channel.setSound(null, null)
        case SoundVibra =>
          channel.enableVibration(true)
          channel.setVibrationPattern(vibrationPattern)
      }
      channel
    } else null
  }

  private def buildForegroundChannel(): NotificationChannel = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = new NotificationChannel(
        channelIDForeground,
        "Foreground Service",
        NotificationManager.IMPORTANCE_NONE
      )
      channel.setShowBadge(false) // 禁用通知圆点
      channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET)
      channel
    } else null
  }
}

object Notifications {
  sealed class EffectType private[Notifications] (val value: Int)

  case object Mute       extends EffectType(1)
  case object Sound      extends EffectType(2)
  case object Vibration  extends EffectType(3)
  case object SoundVibra extends EffectType(4)
  case object Disabled   extends EffectType(0)

  object EffectType {

    def apply(value: Int): EffectType = value match {
      case Disabled.value   => Disabled
      case Mute.value       => Mute
      case Sound.value      => Sound
      case Vibration.value  => Vibration
      case SoundVibra.value => SoundVibra
      case _                => Vibration
    }
  }

}
