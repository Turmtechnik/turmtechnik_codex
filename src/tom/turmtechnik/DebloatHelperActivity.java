package tom.turmtechnik;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Zeigt eine Anleitung und exportiert das "Heavy Debloat" ADB-Skript als .bat-Datei.
 * Das Skript muss auf einem PC mit verbundenem Gerät (USB, ADB) ausgeführt werden –
 * die App kann pm uninstall nicht selbst ausführen (keine Shell-Rechte).
 */
public class DebloatHelperActivity extends Activity {

    /** Pakete die mit „pm uninstall --user 0“ entfernt werden (Heavy Debloat Script). */
    private static final String[] UNINSTALL_PACKAGES = {
            "android.autoinstalls.config.samsung",
            "com.amazon.avod.thirdpartyclient",
            "com.android.apps.tag",
            "com.android.bips",
            "com.android.bipscom.wsomacp",
            "com.android.bookmarkprovider",
            "com.android.calllogbackup",
            "com.android.chrome",
            "com.android.cts.ctsshim",
            "com.android.cts.priv.ctsshim",
            "com.android.dreams.basic",
            "com.android.dreams.phototable",
            "com.android.dynsystem",
            "com.android.egg",
            "com.android.emergency",
            "com.android.hotspot2",
            "com.android.hotwordenrollment.okgoogle",
            "com.android.hotwordenrollment.xgoogle",
            "com.android.hotwordenrollment.tgoogle",
            "com.android.htmlviewer",
            "com.android.internal.display.cutout.emulation.corner",
            "com.android.internal.display.cutout.emulation.double",
            "com.android.internal.display.cutout.emulation.hole",
            "com.android.internal.display.cutout.emulation.tall",
            "com.android.internal.display.cutout.emulation.waterfall",
            "com.android.managedprovisioning",
            "com.android.printspooler",
            "com.android.providers.partnerbookmarks",
            "com.android.providers.userdictionary",
            "com.android.sharedstoragebackup",
            "com.android.statementservice",
            "com.android.stk",
            "com.android.stk2",
            "com.android.theme.color.black",
            "com.android.theme.color.cinnamon",
            "com.android.theme.color.green",
            "com.android.theme.color.ocean",
            "com.android.theme.color.orchid",
            "com.android.theme.color.purple",
            "com.android.theme.color.space",
            "com.android.theme.font.notoserifsource",
            "com.android.theme.icon.pebble",
            "com.android.theme.icon.roundedrect",
            "com.android.theme.icon.taperedrect",
            "com.android.theme.icon.teardrop",
            "com.android.theme.icon.vessel",
            "com.android.theme.icon_pack.circular.android",
            "com.android.theme.icon_pack.circular.launcher",
            "com.android.theme.icon_pack.circular.settings",
            "com.android.theme.icon_pack.circular.systemui",
            "com.android.theme.icon_pack.circular.themepicker",
            "com.android.theme.icon_pack.filled.android",
            "com.android.theme.icon_pack.filled.launcher",
            "com.android.theme.icon_pack.filled.settings",
            "com.android.theme.icon_pack.filled.systemui",
            "com.android.theme.icon_pack.filled.themepicker",
            "com.android.theme.icon_pack.rounded.android",
            "com.android.theme.icon_pack.rounded.launcher",
            "com.android.theme.icon_pack.rounded.settings",
            "com.android.theme.icon_pack.rounded.systemui",
            "com.android.theme.icon_pack.rounded.themepicker",
            "com.android.traceur",
            "com.android.wallpaperbackup",
            "com.aura.oobe.samsung",
            "com.aura.oobe.samsung.gl",
            "com.diotek.sec.lookup.dictionary",
            "com.dsi.ant.plugins.antplus",
            "com.dsi.ant.sample.acquirechannels",
            "com.dsi.ant.server",
            "com.dsi.ant.service.socket",
            "com.eterno",
            "com.eterno.shortvideos",
            "com.facebook.appmanager",
            "com.facebook.katana",
            "com.facebook.services",
            "com.facebook.system",
            "com.google.android.apps.accessibility.voiceaccess",
            "com.google.android.apps.carrier.carrierwifi",
            "com.google.android.apps.docs",
            "com.google.android.apps.gcs",
            "com.google.android.apps.maps",
            "com.google.android.apps.messaging",
            "com.google.android.apps.photos",
            "com.google.android.apps.restore",
            "com.google.android.apps.setupwizard.searchselector",
            "com.google.android.apps.tachyon",
            "com.google.android.apps.turbo",
            "com.google.android.apps.wellbeing",
            "com.google.android.apps.youtube.music",
            "com.google.android.as",
            "com.google.android.as.oss",
            "com.google.android.cellbroadcastreceiver",
            "com.google.android.configupdater",
            "com.google.android.ext.shared",
            "com.google.android.feedback",
            "com.google.android.gm",
            "com.google.android.gms.location.history",
            "com.google.android.googlequicksearchbox",
            "com.google.android.healthconnect.controller",
            "com.google.android.onetimeinitializer",
            "com.google.android.overlay.modules.ext.services",
            "com.google.android.partnersetup",
            "com.google.android.printservice.recommendation",
            "com.google.android.projection.gearhead",
            "com.google.android.setupwizard",
            "com.google.android.tts",
            "com.google.android.videos",
            "com.google.android.youtube",
            "com.google.ar.core",
            "com.google.audio.hearing.visualization.accessibility.scribe",
            "com.hiya.star",
            "com.knox.vpn.proxyhandler",
            "com.microsoft.appmanager",
            "com.microsoft.office.excel",
            "com.microsoft.office.word",
            "com.microsoft.office.powerpoint",
            "com.microsoft.office.officehubrow",
            "com.microsoft.office.outlook",
            "com.microsoft.skydrive",
            "com.monotype.android.font.foundation",
            "com.monotype.android.font.samsungone",
            "com.mygalaxy",
            "com.netflix.mediaclient",
            "com.netflix.partner.activation",
            "com.opera.max.oem",
            "com.osp.app.signin",
            "com.samsung.aasaservice",
            "com.samsung.android.accessibility.talkback",
            "com.samsung.android.aircommandmanager",
            "com.samsung.android.alive.service",
            "com.samsung.android.allshare.service.mediashare",
            "com.samsung.android.appseparation",
            "com.samsung.android.app.appsedge",
            "com.samsung.android.app.camera.sticker.facearavatar.preload",
            "com.samsung.android.app.clipboardedge",
            "com.samsung.android.app.galaxyfinder",
            "com.samsung.android.app.notes.addons",
            "com.samsung.android.app.omcagent",
            "com.samsung.android.app.reminder",
            "com.samsung.android.app.routines",
            "com.samsung.android.app.sbrowseredge",
            "com.samsung.android.app.settings.bixby",
            "com.samsung.android.app.sharelive",
            "com.samsung.android.app.simplesharing",
            "com.samsung.android.app.social",
            "com.samsung.android.app.soundpicker",
            "com.samsung.android.app.spage",
            "com.samsung.android.app.taskedge",
            "com.samsung.android.app.tips",
            "com.samsung.android.app.watchmanagerstub",
            "com.samsung.android.ardrawing",
            "com.samsung.android.aremoji",
            "com.samsung.android.aremojieditor",
            "com.samsung.android.arzone",
            "com.samsung.android.authfw",
            "com.samsung.android.aware.service",
            "com.samsung.android.bbc.bbcagent",
            "com.samsung.android.beaconmanager",
            "com.samsung.android.bixby.agent",
            "com.samsung.android.bixby.agent.dummy",
            "com.samsung.android.bixby.service",
            "com.samsung.android.bixby.wakeup",
            "com.samsung.android.bixbyvision.framework",
            "com.samsung.android.brightnessbackupservice",
            "com.samsung.android.cidmanager",
            "com.samsung.android.cmfa.framework",
            "com.samsung.android.da.daagent",
            "com.samsung.android.dqagent",
            "com.samsung.android.drivelink.stub",
            "com.samsung.android.dsms",
            "com.samsung.android.dynamiclock",
            "com.samsung.android.galaxycontinuity",
            "com.samsung.android.incallui",
            "com.samsung.android.app.telephonyui",
            "com.samsung.android.dialer",
            "com.sec.phone",
            "com.samsung.android.app.contacts",
            "com.samsung.android.contacts",
            "com.samsung.android.providers.contacts",
            "com.samsung.android.easysetup",
            "com.samsung.android.emojiupdater",
            "com.samsung.android.fmm",
            "com.samsung.android.game.gamehome",
            "com.samsung.android.game.gametools",
            "com.samsung.android.game.gos",
            "com.samsung.android.hdmapp",
            "com.samsung.android.icecone",
            "com.samsung.android.ipsgeofence",
            "com.samsung.android.kidsinstaller",
            "com.samsung.kidsplay",
            "com.samsung.android.app.notes",
            "com.audible.application",
            "com.samsung.android.knox.analytics.uploader",
            "com.samsung.android.knox.attestation",
            "com.samsung.android.knox.containeragent",
            "com.samsung.android.knox.containercore",
            "com.samsung.android.knox.pushmanager",
            "com.samsung.android.livestickers",
            "com.samsung.android.lool",
            "com.samsung.android.mapsagent",
            "com.samsung.android.mateagent",
            "com.samsung.android.mcfds",
            "com.samsung.android.mcfserver",
            "com.samsung.android.mdecservice",
            "com.samsung.android.mdm",
            "com.samsung.android.mdx",
            "com.samsung.android.mfi",
            "com.samsung.android.mobileservice",
            "com.samsung.android.net.wifi.wifiguider",
            "com.samsung.android.oneconnect",
            "com.samsung.android.rubin.app",
            "com.samsung.android.samsungpass",
            "com.samsung.android.samsungpassautofill",
            "com.samsung.android.scloud",
            "com.samsung.android.scs",
            "com.samsung.android.sdk.handwriting",
            "com.samsung.android.sdm.config",
            "com.samsung.android.securitylogagent",
            "com.samsung.android.service.peoplestripe",
            "com.samsung.android.setupindiaservicestnc",
            "com.samsung.android.shortcutbackupservice",
            "com.samsung.android.sm.devicesecurity",
            "com.samsung.android.sm.policy",
            "com.samsung.android.smartcallprovider",
            "com.samsung.android.smartface",
            "com.samsung.android.smartfitting",
            "com.samsung.android.smartswitchassistant",
            "com.samsung.android.spayfw",
            "com.samsung.android.spaymini",
            "com.samsung.android.stickercenter",
            "com.samsung.android.svcagent",
            "com.samsung.android.svoiceime",
            "com.samsung.android.tadownloader",
            "com.samsung.android.tapack.authfw",
            "com.samsung.android.themecenter",
            "com.samsung.android.themestore",
            "com.samsung.android.uds",
            "com.samsung.android.visionarapps",
            "com.samsung.android.visionintelligence",
            "com.samsung.android.visualars",
            "com.samsung.android.voc",
            "com.samsung.app.highlightplayer",
            "com.samsung.discover",
            "com.samsung.ecomm.global.in",
            "com.samsung.faceservice",
            "com.samsung.gpuwatchapp",
            "com.samsung.ipservice",
            "com.samsung.klmsagent",
            "com.samsung.knox.keychain",
            "com.samsung.memorysaver",
            "com.samsung.mlp",
            "com.samsung.rcs",
            "com.samsung.safetyinformation",
            "com.samsung.sait.sohservice",
            "com.samsung.sec.android.application.csc",
            "com.samsung.sec.android.teegris.tui_service",
            "com.samsung.SMT",
            "com.samsung.sree",
            "com.samsung.storyservice",
            "com.samsung.systemui.bixby2",
            "com.samsung.systemui.hidenotch",
            "com.samsung.systemui.hidenotch.withoutcornerround",
            "com.samsung.ucs.agent.ese",
            "com.sec.android.app.billing",
            "com.sec.android.app.chromecustomizations",
            "com.sec.android.app.DataCreate",
            "com.sec.android.app.factorykeystring",
            "com.sec.android.app.fm",
            "com.sec.android.app.hwmoduletest",
            "com.sec.android.app.magnifier",
            "com.sec.android.app.parser",
            "com.sec.android.app.personalization",
            "com.sec.android.app.popupcalculator",
            "com.sec.android.app.ringtoneBR",
            "com.sec.android.app.samsungapps",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.SecSetupWizard",
            "com.sec.android.app.setupwizardlegalprovider",
            "com.sec.android.app.soundalive",
            "com.sec.android.app.suwscriptplayer",
            "com.sec.android.app.vepreload",
            "com.sec.android.app.wlantest",
            "com.sec.android.autodoodle.service",
            "com.sec.android.daemonapp",
            "com.sec.android.diagmonagent",
            "com.sec.android.easyMover",
            "com.sec.android.easyMover.Agent",
            "com.sec.android.easyonehand",
            "com.sec.android.emergencylauncher",
            "com.sec.android.emergencymode.service",
            "com.sec.android.mimage.avatarstickers",
            "com.sec.android.preloadinstaller",
            "com.sec.android.provider.badge",
            "com.sec.android.provider.emergencymode",
            "com.sec.android.service.health",
            "com.sec.android.smartfpsadjuster",
            "com.sec.android.uibcvirtualsoftkey",
            "com.sec.android.widgetapp.easymodecontactswidget",
            "com.sec.android.widgetapp.webmanual",
            "com.sec.bcservice",
            "com.sec.enterprise.knox.attestation",
            "com.sec.enterprise.knox.cloudmdm.smdms",
            "com.sec.enterprise.mdm.services.simpin",
            "com.sec.hearingadjust",
            "com.sec.location.nsflp2",
            "com.sec.mhs.smarttethering",
            "com.sec.mldapchecker",
            "com.sec.spp.push",
            "com.sec.sve",
            "com.sec.android.app.clockpackage",
            "com.samsung.android.app.clockpack",
            "com.sec.penup",
            "com.skype.raider",
            "com.skms.android.agent",
            "com.snapchat.android",
            "com.snap.camerakit.plugin.v1",
            "com.yandex.browser",
            "de.axelspringer.yana.zeropage",
            "flipboard.boxer.app",
            "in.amazon.mShop.android.shopping",
            "ru.yandex.disk",
            "ru.yandex.searchplugin",
            "ru.yandex.yandexmaps",
    };

    private static final String[] INSTALL_EXISTING_PACKAGES = {
            "com.sec.android.soagent",
            "com.sec.android.systemupdate",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debloat_helper);

        Button exportBat = findViewById(R.id.debloat_export_bat);
        Button zurueck = findViewById(R.id.debloat_zurueck);

        exportBat.setOnClickListener(v -> exportBatScript());
        zurueck.setOnClickListener(v -> finish());
    }

    private void exportBatScript() {
        String basePath = TurmtechnikActivity.sdCardPath != null
                ? TurmtechnikActivity.sdCardPath
                : Environment.getExternalStorageDirectory().getPath();
        File dir = new File(basePath, "Turmtechnik");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(this, R.string.debloat_export_fehler_ordner, Toast.LENGTH_LONG).show();
            return;
        }
        File batFile = new File(dir, "HeavyDebloatScript.bat");

        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("echo Heavy Debloat Script by invinciblevenom\r\n");
        sb.append("adb devices\r\n");
        sb.append("\r\n");

        Set<String> seen = new LinkedHashSet<>();
        for (String pkg : UNINSTALL_PACKAGES) {
            if (seen.add(pkg)) {
                sb.append("adb shell pm uninstall --user 0 ").append(pkg).append("\r\n");
            }
        }
        sb.append("\r\n");
        for (String pkg : INSTALL_EXISTING_PACKAGES) {
            sb.append("adb shell cmd package install-existing ").append(pkg).append("\r\n");
        }
        sb.append("echo Killing adb server\r\n");
        sb.append("adb kill-server\r\n");
        sb.append("pause\r\n");

        try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(batFile), StandardCharsets.UTF_8)) {
            w.write(sb.toString());
            w.flush();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.debloat_export_fehler) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, getString(R.string.debloat_export_erfolg, batFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
    }
}
