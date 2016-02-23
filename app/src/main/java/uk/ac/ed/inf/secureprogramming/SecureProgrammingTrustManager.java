package uk.ac.ed.inf.secureprogramming;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.StringBufferInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Implements a trust manager for the Secure Programming server
 */
public class SecureProgrammingTrustManager implements X509TrustManager {
    private static final String TAG = SecureProgrammingTrustManager.class.getSimpleName();

    private X509Certificate self_certificate;
    private static final String CERTIFICATE =
            "-----BEGIN CERTIFICATE-----\n"+
            "MIICLTCCAZYCCQDGURthiyMApDANBgkqhkiG9w0BAQsFADBbMQswCQYDVQQGEwJB\n"+
            "VTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50ZXJuZXQgV2lkZ2l0\n"+
            "cyBQdHkgTHRkMRQwEgYDVQQDDAsxOTIuMTY4LjIuMTAeFw0xNjAyMTExMjM0Mjla\n"+
            "Fw0xNzAyMTAxMjM0MjlaMFsxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0\n"+
            "YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQxFDASBgNVBAMM\n"+
            "CzE5Mi4xNjguMi4xMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDFZ2uoa0zk\n"+
            "eSbMgzeAAyC4jfJIqSnQElNFhAmQpaK1puRO6RBq4faxPkY3I07HFx0hK/ZegRwM\n"+
            "zneCgXBDPxzoEUQ2hYfOTtx+AugRArpVO42Edb1eSM0gz/K/WXZQ6MDqgmg7sHDV\n"+
            "evhrGkBg1uPRrd4ukk+RaPnb+wjCLMaSAQIDAQABMA0GCSqGSIb3DQEBCwUAA4GB\n"+
            "AFEg8J80DREdeWwywiTTIBEXTOgw7sRHvrgUr7l6Ubinih+kxc15LJTeze1Y46tG\n"+
            "tXVZNpvOMKRDM1PSbVzpW8CBd91Vi+PwWIjEESMLBXcFhhHTQItzugz0fnC5Zh82\n"+
            "obl7UQzzHh2TKGoA7lVqTx9SH5qlg4UXlxqG3ZR2ol6I\n"+
            "-----END CERTIFICATE-----";

    public SecureProgrammingTrustManager() throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X509");
        self_certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(CERTIFICATE.getBytes("UTF-8")));
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        throw new UnsupportedOperationException("Client trust not supported.");

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        for (X509Certificate cert : chain) {
            try {
                cert.verify(self_certificate.getPublicKey());
                return;
            } catch (NoSuchAlgorithmException e) {
                throw new CertificateException("Untrusted certificate");
            } catch (InvalidKeyException e) {
                throw new CertificateException("Untrusted certificate");
            } catch (NoSuchProviderException e) {
                throw new CertificateException("Untrusted certificate");
            } catch (SignatureException e) {
                throw new CertificateException("Untrusted certificate");
            }
        }

        throw new CertificateException("Untrusted certificate");
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{self_certificate};
    }
}
