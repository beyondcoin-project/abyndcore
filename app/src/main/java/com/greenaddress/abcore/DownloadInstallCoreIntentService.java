package com.greenaddress.abcore;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

public class DownloadInstallCoreIntentService extends IntentService {

    public static final String PARAM_OUT_MSG = "abtcore";
    private static final String TAG = DownloadInstallCoreIntentService.class.getName();

    public DownloadInstallCoreIntentService() {
        super(DownloadInstallCoreIntentService.class.getName());
    }

    private static String getRnd() {

        final Random ranGen = new SecureRandom();
        final byte[] pass = new byte[16];
        ranGen.nextBytes(pass);
        return Utils.toBase58(pass);
    }

    public static void configureCore(final Context c) throws IOException {

        final File coreConf = new File(Utils.getBitcoinConf(c));
        if (coreConf.exists()) {
            return;
        }
        coreConf.getParentFile().mkdirs();

        FileOutputStream outputStream;

        try {
            outputStream = new FileOutputStream(coreConf);
            outputStream.write("rpcuser=groestlcoinrpc\n".getBytes());
            outputStream.write(String.format("rpcpassword=%s\n", getRnd()).getBytes());
            outputStream.write("listen=1\n".getBytes());

            //outputStream.write("bind=127.0.0.1\n".getBytes());
            outputStream.write("disablewallet=1\n".getBytes());
            outputStream.write("testnet=0\n".getBytes());
            //outputStream.write("testnet=1\n".getBytes());
            //outputStream.write("addnode=192.168.2.47\n".getBytes());
            //outputStream.write("regtest=1\n".getBytes());
            outputStream.write("upnp=0\n".getBytes());
            // don't attempt onion connections by default
            outputStream.write("onlynet=ipv4\n".getBytes());

            // Afaik ipv6 is broken on android, disable by default, user can change this
            // outputStream.write("onlynet=ipv6\n".getBytes());
            outputStream.write(String.format("datadir=%s\n", Utils.getLargestFilesDir(c)).getBytes());

            String addNodes = "addnode=104.236.130.222\n" +
                    "addnode=104.236.133.196\n" +
                    "addnode=104.236.178.245\n" +
                    "addnode=104.45.230.117\n" +
                    "addnode=107.185.177.98\n" +
                    "addnode=141.105.70.64\n" +
                    "addnode=144.76.239.66\n" +
                    "addnode=149.210.226.49\n" +
                    "addnode=167.114.208.68\n" +
                    "addnode=188.134.72.213\n" +
                    "addnode=192.99.6.207\n" +
                    "addnode=193.136.98.184\n" +
                    "addnode=198.199.105.43\n" +
                    "addnode=201.20.78.54\n" +
                    "addnode=212.93.226.171\n" +
                    "addnode=23.21.204.34\n" +
                    "addnode=37.59.24.15\n" +
                    "addnode=37.97.177.81\n" +
                    "addnode=5.9.39.9\n" +
                    "addnode=52.32.48.24\n" +
                    "addnode=72.46.152.250\n" +
                    "addnode=77.164.238.146\n" +
                    "addnode=83.243.92.110\n" +
                    "addnode=83.83.161.46\n" +
                    "addnode=83.84.24.180\n" +
                    "addnode=84.101.108.11\n" +
                    "addnode=84.101.108.233\n" +
                    "addnode=84.101.108.238\n" +
                    "addnode=84.101.108.29\n" +
                    "addnode=85.11.137.70\n" +
                    "addnode=87.98.185.244\n" +
                    "addnode=88.193.138.220\n" +
                    "addnode=92.113.137.67\n" +
                    "addnode=92.113.14.11\n" +
                    "addnode=92.113.17.50\n" +
                    "addnode=92.113.31.29\n" +
                    "addnode=92.113.42.88\n" +
                    "addnode=92.113.53.175\n" +
                    "addnode=92.113.57.28\n" +
                    "addnode=94.23.33.122\n" +
                    "addnode=94.23.55.211\n" +
                    "addnode=108.68.65.71\n" +
                    "addnode=79.237.233.187\n" +
                    "addnode=79.237.234.153\n" +
                    "addnode=79.249.117.58\n" +
                    "addnode=79.249.121.244\n" +
                    "addnode=79.252.224.3\n" +
                    "addnode=79.252.227.90\n" +
                    "addnode=79.252.238.175\n" +
                    "addnode=79.252.238.242\n" +
                    "addnode=80.64.86.17";

            outputStream.write(addNodes.getBytes());

            IOUtils.closeQuietly(outputStream);
        } catch (final IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private List<Packages.PkgH> getPackages() {
        final String arch = Utils.getArch();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Boolean archEnabled = prefs.getBoolean("archisenabled", false);
        return archEnabled ? Packages.getArchPackages(arch) : Packages.getDebPackages(arch);
    }
    private List<Packages.PkgH> getPackagesG() {
        final String arch = Utils.getArch();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Boolean archEnabled = prefs.getBoolean("archisenabled", false);
        return Packages.getDebPackagesG(arch);
    }
    @Override
    protected void onHandleIntent(final Intent intent) {
        // this already runs in its own thread but no reasons the pkgs couldn't be handle concurrently.
        final File dir = Utils.getDir(DownloadInstallCoreIntentService.this);
        final String arch = Utils.getArch();

        final List<Packages.PkgH> pkgs = getPackages();
        final List<Packages.PkgH> pkgsG = getPackagesG();

        try {

            for (final Packages.PkgH d : pkgs) {
                for (final String a : d.archHash) {
                    try {
                        if (a.startsWith(arch)) {
                            unpack(d, arch, dir, a, new Utils.OnDownloadSpeedChange() {
                                @Override
                                public void bytesPerSecondUpdate(final int bytes) {
                                    sendUpdate("Downloading", d, bytes);
                                }
                            });
                            break;
                        }
                    } catch (final FileNotFoundException e) {
                        Log.e(TAG, e.getMessage());
                        Log.e(TAG, "NOT FOUND " + String.format(d.pkg, arch));
                        throw e;
                    }
                }
            }

            for (final Packages.PkgH d : pkgsG) {
                for (final String a : d.archHash) {
                    try {
                        if (a.startsWith(arch)) {
                            unpackG(d, arch, dir, a, new Utils.OnDownloadSpeedChange() {
                                @Override
                                public void bytesPerSecondUpdate(final int bytes) {
                                    sendUpdate("Downloading", d, bytes);
                                }
                            });
                            break;
                        }
                    } catch (final FileNotFoundException e) {
                        Log.e(TAG, e.getMessage());
                        Log.e(TAG, "NOT FOUND " + String.format(d.pkg, arch));
                        throw e;
                    }
                }
            }


            // bitcoin core & deps installed, configure it now
            configureCore(this);

            // notify

            // processing done here….
            final Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.DownloadInstallCoreResponseReceiver.ACTION_RESP);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra(PARAM_OUT_MSG, "OK");
            sendBroadcast(broadcastIntent);


        } catch (final Utils.ValidationFailure | ArchiveException | NoSuchAlgorithmException | IOException e) {
            Log.i(TAG, e.getMessage());
            e.printStackTrace();
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(MainActivity.DownloadInstallCoreResponseReceiver.ACTION_RESP);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.putExtra(PARAM_OUT_MSG, "exception");
            broadcastIntent.putExtra("exception", e.getMessage());

            sendBroadcast(broadcastIntent);
        }

        Log.v(TAG, "onHandleIntent END");
    }

    private void sendUpdate(final String upd, final Packages.PkgH pkg) {
        sendUpdate(upd, pkg, null);
    }

    private void sendUpdate(final String upd, final Packages.PkgH pkg, final Integer bytesPerSec) {
        Log.i(TAG, upd);
        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.DownloadInstallCoreResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, "ABCOREUPDATE");
        broadcastIntent.putExtra("ABCOREUPDATE", getPackages().indexOf(pkg));
        broadcastIntent.putExtra("ABCOREUPDATEMAX", getPackages().size());
        if (bytesPerSec != null) {
            broadcastIntent.putExtra("ABCOREUPDATESPEED", bytesPerSec);
        }

        broadcastIntent.putExtra("ABCOREUPDATETXT", String.format("%s %s", upd, pkg.pkg.substring(pkg.pkg.lastIndexOf("/") + 1)));
        sendBroadcast(broadcastIntent);
    }

    private void unpack(final Packages.PkgH pkg, final String arch, final File outputDir, final String sha256raw, final Utils.OnDownloadSpeedChange odsc) throws IOException, NoSuchAlgorithmException, ArchiveException {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final boolean isArchLinux = prefs.getBoolean("archisenabled", false);

        final String url = Packages.getPackageUrl(pkg, this, arch, isArchLinux);
        final String filePath = Utils.getFilePathFromUrl(this, url);

        // Download file
        sendUpdate("Downloading", pkg);
        Utils.downloadFile(url, filePath, odsc);


        // Verify sha256sum
        sendUpdate("Verifying", pkg);
        Utils.validateSha256sum(arch, sha256raw, filePath);

        // extract from deb/ar file the data.tar.xz, then uncompress via xz and untar
        sendUpdate("Unpacking", pkg);

        if (isArchLinux) {
            Utils.extractTarXz(new File(filePath), outputDir);
        } else {
            Utils.extractDataTarXzFromDeb(new File(filePath), outputDir);
        }
    }
    private void unpackG(final Packages.PkgH pkg, final String arch, final File outputDir, final String sha256raw, final Utils.OnDownloadSpeedChange odsc) throws IOException, NoSuchAlgorithmException, ArchiveException {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final boolean isArchLinux = prefs.getBoolean("archisenabled", false);

        final String url = Packages.getPackageUrlG(pkg, this, arch, isArchLinux);
        final String filePath = Utils.getFilePathFromUrl(this, url);

        // Download file
        sendUpdate("Downloading", pkg);
        Utils.downloadFile(url, filePath, odsc);


        // Verify sha256sum
        sendUpdate("Verifying", pkg);
        Utils.validateSha256sum(arch, sha256raw, filePath);

        // extract from deb/ar file the data.tar.xz, then uncompress via xz and untar
        sendUpdate("Unpacking", pkg);

        if (isArchLinux) {
            Utils.extractTarXz(new File(filePath), outputDir);
        } else {
            Utils.extractDataTarXzFromDeb(new File(filePath), outputDir);
        }
    }
}
