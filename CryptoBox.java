package com.example.iechat;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CryptoBox {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int RSA_BITS = 2048;
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;
    private static final String TAG = "CRYPTOBOX";

    /** Generate RSA keypair if absent */
    public static void ensureKeyPair(String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (!ks.containsAlias(alias)) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);

            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                    .setKeySize(RSA_BITS)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            kpg.initialize(spec);
            kpg.generateKeyPair();
            Log.d(TAG, "KeyPair generated for alias=" + alias);
        }
    }

    /** Get public key in Base64 (X.509 format) */
    public static String getPublicKeyBase64(String alias) throws Exception {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);

            if (!ks.containsAlias(alias)) {
                throw new IllegalStateException("Key alias not found: " + alias);
            }

            java.security.cert.Certificate cert = ks.getCertificate(alias);
            if (cert == null) {
                throw new IllegalStateException("Certificate not found for alias: " + alias);
            }

            PublicKey pub = cert.getPublicKey();
            if (pub == null) {
                throw new IllegalStateException("Public key not found in certificate for alias: " + alias);
            }

            byte[] encodedKey = pub.getEncoded();
            if (encodedKey == null || encodedKey.length == 0) {
                throw new IllegalStateException("Public key encoding failed for alias: " + alias);
            }

            String base64Key = Base64.encodeToString(encodedKey, Base64.NO_WRAP);
            Log.d(TAG, "Public key retrieved for alias=" + alias + ", length=" + base64Key.length() + " chars");

            return base64Key;

        } catch (Exception e) {
            Log.e(TAG, "Failed to get public key for alias: " + alias, e);
            throw e;
        }
    }

    /** Decode recipient public key (Base64 or PEM) */
    private static byte[] decodeRecipientPublicKey(String pemOrBase64) throws IllegalArgumentException {
        try {
            if (pemOrBase64 == null || pemOrBase64.trim().isEmpty()) {
                throw new IllegalArgumentException("Public key string is null or empty");
            }

            String cleaned = pemOrBase64.trim();

            // Check if it's PEM format
            if (cleaned.startsWith("-----BEGIN") && cleaned.contains("PUBLIC KEY")) {
                // Use regex to extract base64 content
                Pattern pattern = Pattern.compile(
                        "-----BEGIN PUBLIC KEY-----([A-Za-z0-9+/=\\s]+)-----END PUBLIC KEY-----");
                Matcher matcher = pattern.matcher(cleaned);

                if (matcher.find()) {
                    String base64Content = matcher.group(1).trim();
                    base64Content = base64Content.replaceAll("\\s", "");
                    return Base64.decode(base64Content, Base64.NO_WRAP);
                } else {
                    throw new IllegalArgumentException("Invalid PEM format: cannot extract base64 content");
                }
            } else {
                // Assume it's raw base64, just clean and decode
                cleaned = cleaned.replaceAll("\\s", "");
                return Base64.decode(cleaned, Base64.NO_WRAP);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode public key: " + e.getMessage(), e);
        }
    }

    /** Encrypted message container */
    public static class EncryptedMessage {
        public String ciphertext; // Base64
        public String nonce;      // Base64
        public String ek;         // Base64
    }

    /** Encrypt plaintext for recipient */
    public static EncryptedMessage encryptForRecipient(String recipientPubPemOrBase64, String plaintext) throws Exception {
        try {
            debugPublicKey(recipientPubPemOrBase64);

            byte[] pubBytes = decodeRecipientPublicKey(recipientPubPemOrBase64);
            Log.d(TAG, "Decoded public key bytes length: " + pubBytes.length);

            PublicKey recipientPub = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(pubBytes));

            if (!(recipientPub instanceof RSAPublicKey)) {
                throw new IllegalArgumentException("Recipient key is not RSA");
            }

            // AES-256 key
            byte[] aesKeyBytes = new byte[32];
            new SecureRandom().nextBytes(aesKeyBytes);
            SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // AES-GCM nonce
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            new SecureRandom().nextBytes(nonce);

            // AES-GCM encryption
            Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            gcm.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
            byte[] cipherBytes = gcm.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // RSA-OAEP wrap AES key with SHA-256 and MGF1-SHA1 (for compatibility)
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT); // FIXED: MGF1 with SHA1
            rsa.init(Cipher.ENCRYPT_MODE, recipientPub, oaepParams);
            byte[] ekBytes = rsa.doFinal(aesKeyBytes);

            // Build encrypted message
            EncryptedMessage em = new EncryptedMessage();
            em.ciphertext = Base64.encodeToString(cipherBytes, Base64.NO_WRAP);
            em.nonce = Base64.encodeToString(nonce, Base64.NO_WRAP);
            em.ek = Base64.encodeToString(ekBytes, Base64.NO_WRAP);

            Log.d(TAG, "ENCRYPT: Successfully encrypted message");
            return em;

        } catch (Exception e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /** Decrypt hybrid message */
    public static String decryptMessage(String alias, String ekBase64, String nonceBase64, String cipherBase64) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (!ks.containsAlias(alias)) throw new IllegalStateException("Private key alias missing: " + alias);

        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, null);
        if (entry == null) throw new IllegalStateException("Private key entry not found: " + alias);

        PrivateKey priv = entry.getPrivateKey();
        if (priv == null) throw new IllegalStateException("Private key not accessible: " + alias);

        try {
            // Unwrap AES key with explicit OAEP parameters (SHA-256 with MGF1-SHA1 for compatibility)
            byte[] ekBytes = Base64.decode(ekBase64, Base64.NO_WRAP);
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT); // FIXED: MGF1 with SHA1
            rsa.init(Cipher.DECRYPT_MODE, priv, oaepParams);
            byte[] aesKeyBytes = rsa.doFinal(ekBytes);

            // Debug logs
            Log.d(TAG, "DECRYPT: EK length: " + ekBytes.length);
            Log.d(TAG, "DECRYPT: Unwrapped AES key length: " + aesKeyBytes.length);

            // AES-GCM decrypt
            byte[] nonce = Base64.decode(nonceBase64, Base64.NO_WRAP);
            byte[] cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP);

            Log.d(TAG, "DECRYPT: Nonce length: " + nonce.length);
            Log.d(TAG, "DECRYPT: Ciphertext length: " + cipherBytes.length);

            Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            SecretKeySpec aesKeySpec = new SecretKeySpec(aesKeyBytes, "AES");
            gcm.init(Cipher.DECRYPT_MODE, aesKeySpec, gcmSpec);

            byte[] plain = gcm.doFinal(cipherBytes);
            Log.d(TAG, "DECRYPT successful, plaintext=" + new String(plain, StandardCharsets.UTF_8));
            return new String(plain, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /** Get or generate keypair for user */
    public static KeyPair getKeyPairFromKeystore(String userId) throws Exception {
        String alias = "chat_key_" + userId;
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(alias)) {
            try {
                KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
                if (entry != null && entry.getPrivateKey() != null && entry.getCertificate() != null) {
                    Log.d(TAG, "Found existing keypair for alias: " + alias);
                    return new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error accessing existing keypair for " + alias + ", will generate new one", e);
                // Continue to generate new keypair
            }
        }

        // Generate new keypair with SHA-256 digest
        Log.d(TAG, "Generating new keypair with SHA-256 for alias: " + alias);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
        kpg.initialize(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(RSA_BITS)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setRandomizedEncryptionRequired(true)
                .build()
        );

        KeyPair keyPair = kpg.generateKeyPair();
        Log.d(TAG, "Successfully generated new keypair for alias: " + alias);
        return keyPair;
    }

    /** Helper method to check if key exists */
    public static boolean keyExists(String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        return ks.containsAlias(alias);
    }

    /** Helper method to delete key */
    public static void deleteKey(String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(alias)) {
            ks.deleteEntry(alias);
            Log.d(TAG, "Deleted key: " + alias);
        }
    }

    /** Debug method to check public key format */
    public static void debugPublicKey(String pemOrBase64) {
        Log.d(TAG, "Raw public key input: " + pemOrBase64);

        if (pemOrBase64.contains("-----BEGIN PUBLIC KEY-----")) {
            Log.d(TAG, "Key appears to be in PEM format");
        } else {
            Log.d(TAG, "Key appears to be in raw Base64 format");
        }

        try {
            byte[] decoded = decodeRecipientPublicKey(pemOrBase64);
            Log.d(TAG, "Successfully decoded public key, length: " + decoded.length + " bytes");

            // Try to create PublicKey object to verify it's valid
            PublicKey pubKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
            Log.d(TAG, "Successfully created PublicKey object: " + pubKey.getAlgorithm());

        } catch (Exception e) {
            Log.e(TAG, "Public key decoding failed: " + e.getMessage(), e);
        }
    }

    /** Helper method to clean up old keys and regenerate new ones */
    public static void regenerateKey(String alias) throws Exception {
        deleteKey(alias);
        ensureKeyPair(alias);
        Log.d(TAG, "Regenerated key: " + alias);
    }
}