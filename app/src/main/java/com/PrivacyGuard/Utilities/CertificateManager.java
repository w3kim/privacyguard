package com.PrivacyGuard.Utilities;

import org.sandrop.webscarab.plugin.proxy.SSLSocketFactoryFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.util.Date;
import java.util.Enumeration;

import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

/**
 * Created by Near on 15-01-06.
 */
public class CertificateManager {

    static SSLSocketFactoryFactory mFactoryFactory;
  // generate CA certificate but return a ssl socket factory factory which use this certificate
  public static SSLSocketFactoryFactory generateCACertificate(String dir, String caName, String certName, String KeyType, char[] password) {
      if (mFactoryFactory == null) {
          try {
              mFactoryFactory = new SSLSocketFactoryFactory(dir + "/" + caName, dir + "/" + certName, KeyType, password);
          } catch (GeneralSecurityException | IOException e) {
              e.printStackTrace();
          }
    }
      return mFactoryFactory;
  }

  public static boolean isCACertificateInstalled(String dir, String caName, String type, String password) throws KeyStoreException {
    File fileCA = new File(dir + "/" + caName);
    KeyStore keyStoreCA = null;
    try {
      keyStoreCA = KeyStore.getInstance(type, "BC");
    } catch (KeyStoreException e) {
      e.printStackTrace();
    } catch (NoSuchProviderException e) {
      e.printStackTrace();
    }

    if (fileCA.exists() && fileCA.canRead()) {
      try {
        FileInputStream fileCert = new FileInputStream(fileCA);
        keyStoreCA.load(fileCert, password.toCharArray());
        fileCert.close();
      } catch (NoSuchAlgorithmException | IOException | java.security.cert.CertificateException e) {
        e.printStackTrace();
      }
      Enumeration ex = keyStoreCA.aliases();
      Date exportFilename = null;
      String caAliasValue = "";

      while (ex.hasMoreElements()) {
        String is = (String) ex.nextElement();
        Date lastStoredDate = keyStoreCA.getCreationDate(is);
        if (exportFilename == null || lastStoredDate.after(exportFilename)) {
          exportFilename = lastStoredDate;
          caAliasValue = is;
        }
      }

      try {
          return keyStoreCA.getKey(caAliasValue, password.toCharArray()) != null;
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      } catch (UnrecoverableKeyException e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  // get the CA certificate by the path
  public static X509Certificate getCACertificate(String dir, String caName) {
    String CERT_FILE = dir + "/" + caName + "_export.crt";
    File certFile = new File(CERT_FILE);
    FileInputStream certIs = null;
    try {
      certIs = new FileInputStream(CERT_FILE);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    byte [] cert = new byte[(int)certFile.length()];
    try {
      certIs.read(cert);
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      return X509Certificate.getInstance(cert);
    } catch (CertificateException e) {
      e.printStackTrace();
      return null;
    }
  }
}
