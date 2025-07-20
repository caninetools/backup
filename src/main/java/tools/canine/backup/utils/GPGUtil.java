package tools.canine.backup.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import java.io.*;
import java.security.Security;

public class GPGUtil {

    private static final Logger logger = LogManager.getLogger(GPGUtil.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static boolean encryptFile(String input, String output, boolean armor, boolean withIntegrityCheck) {
        try (OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(output))) {
            PGPPublicKey encKey = readPublicKey();
            if (encKey == null) {
                logger.error("Encryption key not found!!!");
                return false;
            }

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
            try (OutputStream compressedOut = comData.open(bOut)) {
                PGPUtil.writeFileToLiteralData(
                        compressedOut,
                        PGPLiteralData.BINARY,
                        new File(input)
                );
            } finally {
                comData.close();
            }

            byte[] bytes = bOut.toByteArray();
            try (OutputStream out = armor ? new ArmoredOutputStream(fileOut) : fileOut) {
                PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                        new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                                .setWithIntegrityPacket(withIntegrityCheck)
                                .setSecureRandom(new java.security.SecureRandom())
                                .setProvider("BC")
                );
                encryptedDataGenerator.addMethod(
                        new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC")
                );

                try (OutputStream encryptedOut = encryptedDataGenerator.open(out, bytes.length)) {
                    encryptedOut.write(bytes);
                }
            }

            return true;
        } catch (IOException | PGPException exception) {
            logger.error("Unable to encrypt file", exception);
            RequestUtil.sendAlert("Failed Encryption", exception.getMessage(), "high");
            return false;
        }
    }

    private static PGPPublicKey readPublicKey() {
        try (InputStream keyIn = new BufferedInputStream(new FileInputStream("public.pgp")); InputStream decoderStream = PGPUtil.getDecoderStream(keyIn)) {
            PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());

            for (PGPPublicKeyRing keyRing : pgpPub) {
                for (PGPPublicKey key : keyRing) {
                    if (key.isEncryptionKey()) {
                        return key;
                    }
                }
            }
        } catch (IOException | PGPException exception) {
            logger.error("Failed to read public key", exception);
            RequestUtil.sendAlert("Failed Encryption", exception.getMessage(), "high");
        }

        return null;
    }
}
