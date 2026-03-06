@echo off
chcp 65001 >nul
setlocal

REM ADB suchen: zuerst im PATH, sonst im typischen Android-Studio-Pfad
where adb >nul 2>&1
if %ERRORLEVEL% equ 0 goto :adb_ok
set "ADB_DIR=%LOCALAPPDATA%\Android\Sdk\platform-tools"
if exist "%ADB_DIR%\adb.exe" (
  set "PATH=%ADB_DIR%;%PATH%"
  goto :adb_ok
)
set "ADB_DIR=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools"
if exist "%ADB_DIR%\adb.exe" (
  set "PATH=%ADB_DIR%;%PATH%"
  goto :adb_ok
)
if defined ANDROID_HOME (
  set "ADB_DIR=%ANDROID_HOME%\platform-tools"
  if exist "%ADB_DIR%\adb.exe" (
    set "PATH=%ADB_DIR%;%PATH%"
    goto :adb_ok
  )
)
if defined ANDROID_SDK_ROOT (
  set "ADB_DIR=%ANDROID_SDK_ROOT%\platform-tools"
  if exist "%ADB_DIR%\adb.exe" (
    set "PATH=%ADB_DIR%;%PATH%"
    goto :adb_ok
  )
)
echo FEHLER: adb wurde nicht gefunden.
echo Bitte Android SDK Platform-Tools installieren oder den Ordner mit adb.exe in die System-PATH-Variable aufnehmen.
echo Typischer Pfad: %LOCALAPPDATA%\Android\Sdk\platform-tools
echo.
pause
exit /b 1
:adb_ok

echo ============================================
echo Turmtechnik-Tablet Debloat (ADB)
echo Vorinstallierte Apps entfernen - inkl. Google
echo ============================================
echo.
echo Voraussetzung: Tablet per USB verbunden, USB-Debugging an.
echo.
echo NICHT ENTFERNEN (Android/Tablet braucht sie):
echo   com.android.settings, com.android.systemui,
echo   com.android.permissioncontroller
echo.
adb devices
echo.
echo Speichere Liste installierter Pakete in installiert_vorher.txt ...
adb shell pm list packages > installiert_vorher.txt
echo Liste gespeichert. Starte Deinstallieren ...
echo.

adb shell pm uninstall --user 0 android.autoinstalls.config.samsung
adb shell pm uninstall --user 0 com.amazon.avod.thirdpartyclient
adb shell pm uninstall --user 0 com.android.apps.tag
adb shell pm uninstall --user 0 com.android.bips
adb shell pm uninstall --user 0 com.android.bipscom.wsomacp
adb shell pm uninstall --user 0 com.android.bookmarkprovider
adb shell pm uninstall --user 0 com.android.calllogbackup
adb shell pm uninstall --user 0 com.android.chrome
adb shell pm uninstall --user 0 com.android.cts.ctsshim
adb shell pm uninstall --user 0 com.android.cts.priv.ctsshim
adb shell pm uninstall --user 0 com.android.dreams.basic
adb shell pm uninstall --user 0 com.android.dreams.phototable
adb shell pm uninstall --user 0 com.android.dynsystem
adb shell pm uninstall --user 0 com.android.egg
adb shell pm uninstall --user 0 com.android.emergency
adb shell pm uninstall --user 0 com.android.hotspot2
adb shell pm uninstall --user 0 com.android.hotwordenrollment.okgoogle
adb shell pm uninstall --user 0 com.android.hotwordenrollment.xgoogle
adb shell pm uninstall --user 0 com.android.hotwordenrollment.tgoogle
adb shell pm uninstall --user 0 com.android.htmlviewer
adb shell pm uninstall --user 0 com.android.internal.display.cutout.emulation.corner
adb shell pm uninstall --user 0 com.android.internal.display.cutout.emulation.double
adb shell pm uninstall --user 0 com.android.internal.display.cutout.emulation.hole
adb shell pm uninstall --user 0 com.android.internal.display.cutout.emulation.tall
adb shell pm uninstall --user 0 com.android.internal.display.cutout.emulation.waterfall
adb shell pm uninstall --user 0 com.android.managedprovisioning
adb shell pm uninstall --user 0 com.android.printspooler
adb shell pm uninstall --user 0 com.android.providers.partnerbookmarks
adb shell pm uninstall --user 0 com.android.providers.userdictionary
adb shell pm uninstall --user 0 com.android.sharedstoragebackup
adb shell pm uninstall --user 0 com.android.statementservice
adb shell pm uninstall --user 0 com.android.stk
adb shell pm uninstall --user 0 com.android.stk2
adb shell pm uninstall --user 0 com.android.theme.color.black
adb shell pm uninstall --user 0 com.android.theme.color.cinnamon
adb shell pm uninstall --user 0 com.android.theme.color.green
adb shell pm uninstall --user 0 com.android.theme.color.ocean
adb shell pm uninstall --user 0 com.android.theme.color.orchid
adb shell pm uninstall --user 0 com.android.theme.color.purple
adb shell pm uninstall --user 0 com.android.theme.color.space
adb shell pm uninstall --user 0 com.android.theme.font.notoserifsource
adb shell pm uninstall --user 0 com.android.theme.icon.pebble
adb shell pm uninstall --user 0 com.android.theme.icon.roundedrect
adb shell pm uninstall --user 0 com.android.theme.icon.taperedrect
adb shell pm uninstall --user 0 com.android.theme.icon.teardrop
adb shell pm uninstall --user 0 com.android.theme.icon.vessel
adb shell pm uninstall --user 0 com.android.theme.icon_pack.circular.android
adb shell pm uninstall --user 0 com.android.theme.icon_pack.circular.launcher
adb shell pm uninstall --user 0 com.android.theme.icon_pack.circular.settings
adb shell pm uninstall --user 0 com.android.theme.icon_pack.circular.systemui
adb shell pm uninstall --user 0 com.android.theme.icon_pack.circular.themepicker
adb shell pm uninstall --user 0 com.android.theme.icon_pack.filled.android
adb shell pm uninstall --user 0 com.android.theme.icon_pack.filled.launcher
adb shell pm uninstall --user 0 com.android.theme.icon_pack.filled.settings
adb shell pm uninstall --user 0 com.android.theme.icon_pack.filled.systemui
adb shell pm uninstall --user 0 com.android.theme.icon_pack.filled.themepicker
adb shell pm uninstall --user 0 com.android.theme.icon_pack.rounded.android
adb shell pm uninstall --user 0 com.android.theme.icon_pack.rounded.launcher
adb shell pm uninstall --user 0 com.android.theme.icon_pack.rounded.settings
adb shell pm uninstall --user 0 com.android.theme.icon_pack.rounded.systemui
adb shell pm uninstall --user 0 com.android.theme.icon_pack.rounded.themepicker
adb shell pm uninstall --user 0 com.android.traceur
adb shell pm uninstall --user 0 com.android.wallpaperbackup
adb shell pm uninstall --user 0 com.aura.oobe.samsung
adb shell pm uninstall --user 0 com.aura.oobe.samsung.gl
adb shell pm uninstall --user 0 com.diotek.sec.lookup.dictionary
adb shell pm uninstall --user 0 com.dsi.ant.plugins.antplus
adb shell pm uninstall --user 0 com.dsi.ant.sample.acquirechannels
adb shell pm uninstall --user 0 com.dsi.ant.server
adb shell pm uninstall --user 0 com.dsi.ant.service.socket
adb shell pm uninstall --user 0 com.eterno
adb shell pm uninstall --user 0 com.eterno.shortvideos
adb shell pm uninstall --user 0 com.facebook.appmanager
adb shell pm uninstall --user 0 com.facebook.katana
adb shell pm uninstall --user 0 com.facebook.services
adb shell pm uninstall --user 0 com.facebook.system
adb shell pm uninstall --user 0 com.google.android.apps.accessibility.voiceaccess
adb shell pm uninstall --user 0 com.google.android.apps.carrier.carrierwifi
adb shell pm uninstall --user 0 com.google.android.apps.docs
adb shell pm uninstall --user 0 com.google.android.apps.gcs
adb shell pm uninstall --user 0 com.google.android.apps.maps
adb shell pm uninstall --user 0 com.google.android.apps.messaging
adb shell pm uninstall --user 0 com.google.android.apps.photos
adb shell pm uninstall --user 0 com.google.android.apps.restore
adb shell pm uninstall --user 0 com.google.android.apps.setupwizard.searchselector
adb shell pm uninstall --user 0 com.google.android.apps.tachyon
adb shell pm uninstall --user 0 com.google.android.apps.turbo
adb shell pm uninstall --user 0 com.google.android.apps.wellbeing
adb shell pm uninstall --user 0 com.google.android.apps.youtube.music
adb shell pm uninstall --user 0 com.google.android.as
adb shell pm uninstall --user 0 com.google.android.as.oss
adb shell pm uninstall --user 0 com.google.android.cellbroadcastreceiver
adb shell pm uninstall --user 0 com.google.android.configupdater
adb shell pm uninstall --user 0 com.google.android.ext.shared
adb shell pm uninstall --user 0 com.google.android.feedback
adb shell pm uninstall --user 0 com.google.android.gm
adb shell pm uninstall --user 0 com.google.android.gms.location.history
adb shell pm uninstall --user 0 com.google.android.googlequicksearchbox
adb shell pm uninstall --user 0 com.google.android.healthconnect.controller
adb shell pm uninstall --user 0 com.google.android.onetimeinitializer
adb shell pm uninstall --user 0 com.google.android.overlay.modules.ext.services
adb shell pm uninstall --user 0 com.google.android.partnersetup
adb shell pm uninstall --user 0 com.google.android.printservice.recommendation
adb shell pm uninstall --user 0 com.google.android.projection.gearhead
adb shell pm uninstall --user 0 com.google.android.setupwizard
adb shell pm uninstall --user 0 com.google.android.tts
adb shell pm uninstall --user 0 com.google.android.videos
adb shell pm uninstall --user 0 com.google.android.youtube
adb shell pm uninstall --user 0 com.google.ar.core
adb shell pm uninstall --user 0 com.google.audio.hearing.visualization.accessibility.scribe
adb shell pm uninstall --user 0 com.hiya.star
adb shell pm uninstall --user 0 com.knox.vpn.proxyhandler
adb shell pm uninstall --user 0 com.microsoft.appmanager
adb shell pm uninstall --user 0 com.microsoft.office.excel
adb shell pm uninstall --user 0 com.microsoft.office.word
adb shell pm uninstall --user 0 com.microsoft.office.powerpoint
adb shell pm uninstall --user 0 com.microsoft.office.officehubrow
adb shell pm uninstall --user 0 com.microsoft.office.outlook
adb shell pm uninstall --user 0 com.microsoft.skydrive
adb shell pm uninstall --user 0 com.monotype.android.font.foundation
adb shell pm uninstall --user 0 com.monotype.android.font.samsungone
adb shell pm uninstall --user 0 com.mygalaxy
adb shell pm uninstall --user 0 com.netflix.mediaclient
adb shell pm uninstall --user 0 com.netflix.partner.activation
adb shell pm uninstall --user 0 com.opera.max.oem
adb shell pm uninstall --user 0 com.osp.app.signin
adb shell pm uninstall --user 0 com.samsung.aasaservice
adb shell pm uninstall --user 0 com.samsung.android.accessibility.talkback
adb shell pm uninstall --user 0 com.samsung.android.aircommandmanager
adb shell pm uninstall --user 0 com.samsung.android.alive.service
adb shell pm uninstall --user 0 com.samsung.android.allshare.service.mediashare
adb shell pm uninstall --user 0 com.samsung.android.appseparation
adb shell pm uninstall --user 0 com.samsung.android.app.appsedge
adb shell pm uninstall --user 0 com.samsung.android.app.camera.sticker.facearavatar.preload
adb shell pm uninstall --user 0 com.samsung.android.app.clipboardedge
adb shell pm uninstall --user 0 com.samsung.android.app.galaxyfinder
adb shell pm uninstall --user 0 com.samsung.android.app.notes.addons
adb shell pm uninstall --user 0 com.samsung.android.app.omcagent
adb shell pm uninstall --user 0 com.samsung.android.app.reminder
adb shell pm uninstall --user 0 com.samsung.android.app.routines
adb shell pm uninstall --user 0 com.samsung.android.app.sbrowseredge
adb shell pm uninstall --user 0 com.samsung.android.app.settings.bixby
adb shell pm uninstall --user 0 com.samsung.android.app.sharelive
adb shell pm uninstall --user 0 com.samsung.android.app.simplesharing
adb shell pm uninstall --user 0 com.samsung.android.app.social
adb shell pm uninstall --user 0 com.samsung.android.app.soundpicker
adb shell pm uninstall --user 0 com.samsung.android.app.spage
adb shell pm uninstall --user 0 com.samsung.android.app.taskedge
adb shell pm uninstall --user 0 com.samsung.android.app.tips
adb shell pm uninstall --user 0 com.samsung.android.app.watchmanagerstub
adb shell pm uninstall --user 0 com.samsung.android.ardrawing
adb shell pm uninstall --user 0 com.samsung.android.aremoji
adb shell pm uninstall --user 0 com.samsung.android.aremojieditor
adb shell pm uninstall --user 0 com.samsung.android.arzone
adb shell pm uninstall --user 0 com.samsung.android.authfw
adb shell pm uninstall --user 0 com.samsung.android.aware.service
adb shell pm uninstall --user 0 com.samsung.android.bbc.bbcagent
adb shell pm uninstall --user 0 com.samsung.android.beaconmanager
adb shell pm uninstall --user 0 com.samsung.android.bixby.agent
adb shell pm uninstall --user 0 com.samsung.android.bixby.agent.dummy
adb shell pm uninstall --user 0 com.samsung.android.bixby.service
adb shell pm uninstall --user 0 com.samsung.android.bixby.wakeup
adb shell pm uninstall --user 0 com.samsung.android.bixbyvision.framework
adb shell pm uninstall --user 0 com.samsung.android.brightnessbackupservice
adb shell pm uninstall --user 0 com.samsung.android.cidmanager
adb shell pm uninstall --user 0 com.samsung.android.cmfa.framework
adb shell pm uninstall --user 0 com.samsung.android.da.daagent
adb shell pm uninstall --user 0 com.samsung.android.dqagent
adb shell pm uninstall --user 0 com.samsung.android.drivelink.stub
adb shell pm uninstall --user 0 com.samsung.android.dsms
adb shell pm uninstall --user 0 com.samsung.android.dynamiclock
adb shell pm uninstall --user 0 com.samsung.android.galaxycontinuity
adb shell pm uninstall --user 0 com.samsung.android.incallui
adb shell pm uninstall --user 0 com.samsung.android.app.telephonyui
adb shell pm uninstall --user 0 com.samsung.android.dialer
adb shell pm uninstall --user 0 com.sec.phone
adb shell pm uninstall --user 0 com.samsung.android.app.contacts
adb shell pm uninstall --user 0 com.samsung.android.contacts
adb shell pm uninstall --user 0 com.samsung.android.providers.contacts
adb shell pm uninstall --user 0 com.samsung.android.easysetup
adb shell pm uninstall --user 0 com.samsung.android.emojiupdater
adb shell pm uninstall --user 0 com.samsung.android.fmm
adb shell pm uninstall --user 0 com.samsung.android.game.gamehome
adb shell pm uninstall --user 0 com.samsung.android.game.gametools
adb shell pm uninstall --user 0 com.samsung.android.game.gos
adb shell pm uninstall --user 0 com.samsung.android.hdmapp
adb shell pm uninstall --user 0 com.samsung.android.icecone
adb shell pm uninstall --user 0 com.samsung.android.ipsgeofence
adb shell pm uninstall --user 0 com.samsung.android.kidsinstaller
adb shell pm uninstall --user 0 com.samsung.kidsplay
adb shell pm uninstall --user 0 com.samsung.android.app.notes
adb shell pm uninstall --user 0 com.audible.application
adb shell pm uninstall --user 0 com.samsung.android.knox.analytics.uploader
adb shell pm uninstall --user 0 com.samsung.android.knox.attestation
adb shell pm uninstall --user 0 com.samsung.android.knox.containeragent
adb shell pm uninstall --user 0 com.samsung.android.knox.containercore
adb shell pm uninstall --user 0 com.samsung.android.knox.pushmanager
adb shell pm uninstall --user 0 com.samsung.android.livestickers
adb shell pm uninstall --user 0 com.samsung.android.lool
adb shell pm uninstall --user 0 com.samsung.android.mapsagent
adb shell pm uninstall --user 0 com.samsung.android.mateagent
adb shell pm uninstall --user 0 com.samsung.android.mcfds
adb shell pm uninstall --user 0 com.samsung.android.mcfserver
adb shell pm uninstall --user 0 com.samsung.android.mdecservice
adb shell pm uninstall --user 0 com.samsung.android.mdm
adb shell pm uninstall --user 0 com.samsung.android.mdx
adb shell pm uninstall --user 0 com.samsung.android.mfi
adb shell pm uninstall --user 0 com.samsung.android.mobileservice
adb shell pm uninstall --user 0 com.samsung.android.net.wifi.wifiguider
adb shell pm uninstall --user 0 com.samsung.android.oneconnect
adb shell pm uninstall --user 0 com.samsung.android.rubin.app
adb shell pm uninstall --user 0 com.samsung.android.samsungpass
adb shell pm uninstall --user 0 com.samsung.android.samsungpassautofill
adb shell pm uninstall --user 0 com.samsung.android.scloud
adb shell pm uninstall --user 0 com.samsung.android.scs
adb shell pm uninstall --user 0 com.samsung.android.sdk.handwriting
adb shell pm uninstall --user 0 com.samsung.android.sdm.config
adb shell pm uninstall --user 0 com.samsung.android.securitylogagent
adb shell pm uninstall --user 0 com.samsung.android.service.peoplestripe
adb shell pm uninstall --user 0 com.samsung.android.setupindiaservicestnc
adb shell pm uninstall --user 0 com.samsung.android.shortcutbackupservice
adb shell pm uninstall --user 0 com.samsung.android.sm.devicesecurity
adb shell pm uninstall --user 0 com.samsung.android.sm.policy
adb shell pm uninstall --user 0 com.samsung.android.smartcallprovider
adb shell pm uninstall --user 0 com.samsung.android.smartface
adb shell pm uninstall --user 0 com.samsung.android.smartfitting
adb shell pm uninstall --user 0 com.samsung.android.smartswitchassistant
adb shell pm uninstall --user 0 com.samsung.android.spayfw
adb shell pm uninstall --user 0 com.samsung.android.spaymini
adb shell pm uninstall --user 0 com.samsung.android.stickercenter
adb shell pm uninstall --user 0 com.samsung.android.svcagent
adb shell pm uninstall --user 0 com.samsung.android.svoiceime
adb shell pm uninstall --user 0 com.samsung.android.tadownloader
adb shell pm uninstall --user 0 com.samsung.android.tapack.authfw
adb shell pm uninstall --user 0 com.samsung.android.themecenter
adb shell pm uninstall --user 0 com.samsung.android.themestore
adb shell pm uninstall --user 0 com.samsung.android.uds
adb shell pm uninstall --user 0 com.samsung.android.visionarapps
adb shell pm uninstall --user 0 com.samsung.android.visionintelligence
adb shell pm uninstall --user 0 com.samsung.android.visualars
adb shell pm uninstall --user 0 com.samsung.android.voc
adb shell pm uninstall --user 0 com.samsung.app.highlightplayer
adb shell pm uninstall --user 0 com.samsung.discover
adb shell pm uninstall --user 0 com.samsung.ecomm.global.in
adb shell pm uninstall --user 0 com.samsung.faceservice
adb shell pm uninstall --user 0 com.samsung.gpuwatchapp
adb shell pm uninstall --user 0 com.samsung.ipservice
adb shell pm uninstall --user 0 com.samsung.klmsagent
adb shell pm uninstall --user 0 com.samsung.knox.keychain
adb shell pm uninstall --user 0 com.samsung.memorysaver
adb shell pm uninstall --user 0 com.samsung.mlp
adb shell pm uninstall --user 0 com.samsung.rcs
adb shell pm uninstall --user 0 com.samsung.safetyinformation
adb shell pm uninstall --user 0 com.samsung.sait.sohservice
adb shell pm uninstall --user 0 com.samsung.sec.android.application.csc
adb shell pm uninstall --user 0 com.samsung.sec.android.teegris.tui_service
adb shell pm uninstall --user 0 com.samsung.SMT
adb shell pm uninstall --user 0 com.samsung.sree
adb shell pm uninstall --user 0 com.samsung.storyservice
adb shell pm uninstall --user 0 com.samsung.systemui.bixby2
adb shell pm uninstall --user 0 com.samsung.systemui.hidenotch
adb shell pm uninstall --user 0 com.samsung.systemui.hidenotch.withoutcornerround
adb shell pm uninstall --user 0 com.samsung.ucs.agent.ese
adb shell pm uninstall --user 0 com.sec.android.app.billing
adb shell pm uninstall --user 0 com.sec.android.app.chromecustomizations
adb shell pm uninstall --user 0 com.sec.android.app.DataCreate
adb shell pm uninstall --user 0 com.sec.android.app.factorykeystring
adb shell pm uninstall --user 0 com.sec.android.app.fm
adb shell pm uninstall --user 0 com.sec.android.app.hwmoduletest
adb shell pm uninstall --user 0 com.sec.android.app.magnifier
adb shell pm uninstall --user 0 com.sec.android.app.parser
adb shell pm uninstall --user 0 com.sec.android.app.personalization
adb shell pm uninstall --user 0 com.sec.android.app.popupcalculator
adb shell pm uninstall --user 0 com.sec.android.app.ringtoneBR
adb shell pm uninstall --user 0 com.sec.android.app.samsungapps
adb shell pm uninstall --user 0 com.sec.android.app.sbrowser
adb shell pm uninstall --user 0 com.sec.android.app.SecSetupWizard
adb shell pm uninstall --user 0 com.sec.android.app.setupwizardlegalprovider
adb shell pm uninstall --user 0 com.sec.android.app.soundalive
adb shell pm uninstall --user 0 com.sec.android.app.suwscriptplayer
adb shell pm uninstall --user 0 com.sec.android.app.vepreload
adb shell pm uninstall --user 0 com.sec.android.app.wlantest
adb shell pm uninstall --user 0 com.sec.android.autodoodle.service
adb shell pm uninstall --user 0 com.sec.android.daemonapp
adb shell pm uninstall --user 0 com.sec.android.diagmonagent
adb shell pm uninstall --user 0 com.sec.android.easyMover
adb shell pm uninstall --user 0 com.sec.android.easyMover.Agent
adb shell pm uninstall --user 0 com.sec.android.easyonehand
adb shell pm uninstall --user 0 com.sec.android.emergencylauncher
adb shell pm uninstall --user 0 com.sec.android.emergencymode.service
adb shell pm uninstall --user 0 com.sec.android.mimage.avatarstickers
adb shell pm uninstall --user 0 com.sec.android.preloadinstaller
adb shell pm uninstall --user 0 com.sec.android.provider.badge
adb shell pm uninstall --user 0 com.sec.android.provider.emergencymode
adb shell pm uninstall --user 0 com.sec.android.service.health
adb shell pm uninstall --user 0 com.sec.android.smartfpsadjuster
adb shell pm uninstall --user 0 com.sec.android.uibcvirtualsoftkey
adb shell pm uninstall --user 0 com.sec.android.widgetapp.easymodecontactswidget
adb shell pm uninstall --user 0 com.sec.android.widgetapp.webmanual
adb shell pm uninstall --user 0 com.sec.bcservice
adb shell pm uninstall --user 0 com.sec.enterprise.knox.attestation
adb shell pm uninstall --user 0 com.sec.enterprise.knox.cloudmdm.smdms
adb shell pm uninstall --user 0 com.sec.enterprise.mdm.services.simpin
adb shell pm uninstall --user 0 com.sec.hearingadjust
adb shell pm uninstall --user 0 com.sec.location.nsflp2
adb shell pm uninstall --user 0 com.sec.mhs.smarttethering
adb shell pm uninstall --user 0 com.sec.mldapchecker
adb shell pm uninstall --user 0 com.sec.spp.push
adb shell pm uninstall --user 0 com.sec.sve
adb shell pm uninstall --user 0 com.sec.android.app.clockpackage
adb shell pm uninstall --user 0 com.samsung.android.app.clockpack
adb shell pm uninstall --user 0 com.sec.penup
adb shell pm uninstall --user 0 com.skype.raider
adb shell pm uninstall --user 0 com.skms.android.agent
adb shell pm uninstall --user 0 com.snapchat.android
adb shell pm uninstall --user 0 com.snap.camerakit.plugin.v1
adb shell pm uninstall --user 0 com.yandex.browser
adb shell pm uninstall --user 0 de.axelspringer.yana.zeropage
adb shell pm uninstall --user 0 flipboard.boxer.app
adb shell pm uninstall --user 0 in.amazon.mShop.android.shopping
adb shell pm uninstall --user 0 ru.yandex.disk
adb shell pm uninstall --user 0 ru.yandex.searchplugin
adb shell pm uninstall --user 0 ru.yandex.yandexmaps

echo.
echo Samsung System-Update wieder aktivieren ...
adb shell cmd package install-existing com.sec.android.soagent
adb shell cmd package install-existing com.sec.android.systemupdate

echo.
echo Fertig. ADB-Server beenden.
adb kill-server
echo.
pause
