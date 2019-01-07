package com.example.journal;

import android.os.AsyncTask;
import android.util.Log;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.SshTransport;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;

import io.flutter.plugin.common.MethodChannel.Result;

public class GitCloneTask extends AsyncTask<String, Void, Void> {
    private Result result;

    public GitCloneTask(Result _result) {
        result = _result;
    }

    protected Void doInBackground(String... params) {
        String url = params[0];
        String filesDir = params[1];
        File directory = new File(filesDir + "/git");

        Log.d("GitClone Directory", filesDir);

        File keysDir = new File(filesDir + "/keys");
        if (!keysDir.exists()) {
            keysDir.mkdir();
        }
        final String privateKeyPath = filesDir + "/keys/id_rsa";
        final String publicKeyPath = filesDir + "/keys/id_rsa.pub";

        try {
            // Generate key pair
            try {
                JSch jsch = new JSch();
                KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 1024 * 4);

                kpair.writePrivateKey(privateKeyPath);
                kpair.writePublicKey(publicKeyPath, "Auto generated Key");
                kpair.dispose();
            } catch (JSchException ex) {
                Log.d("GitClone", ex.toString());
            } catch (IOException ex) {
                Log.d("GitClone", ex.toString());
            }

            final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
                protected void configure(Host host, Session session) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }

                protected JSch createDefaultJSch(FS fs) throws JSchException {

                    class MyLogger implements com.jcraft.jsch.Logger {
                        java.util.Hashtable name;

                        MyLogger() {
                            name = new java.util.Hashtable();
                            name.put(new Integer(DEBUG), "DEBUG: ");
                            name.put(new Integer(INFO), "INFO: ");
                            name.put(new Integer(WARN), "WARN: ");
                            name.put(new Integer(ERROR), "ERROR: ");
                            name.put(new Integer(FATAL), "FATAL: ");
                        }


                        public boolean isEnabled(int level) {
                            return true;
                        }

                        public void log(int level, String message) {
                            System.err.print(name.get(new Integer(level)));
                            System.err.println(message);
                        }
                    }
                    JSch.setLogger(new MyLogger());

                    JSch defaultJSch = super.createDefaultJSch(fs);
                    defaultJSch.addIdentity(privateKeyPath);

                    Log.d("identityNames", defaultJSch.getIdentityNames().toString());
                    return defaultJSch;
                }
            };

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(directory);

            cloneCommand.setTransportConfigCallback(new TransportConfigCallback() {
                @Override
                public void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });

            cloneCommand.call();
        } catch (TransportException e) {
            // FIXME: Return a better error message?
            System.err.println("Transport Error Cloning repository " + url + " : " + e.getMessage());
            return null;

        } catch (GitAPIException e) {
            System.err.println("Error Cloning repository " + url + " : " + e.getMessage());
            Log.d("gitClone", e.toString());
        }
        return null;
    }

    protected void onPostExecute(Void taskResult) {
        result.success(null);
    }
}