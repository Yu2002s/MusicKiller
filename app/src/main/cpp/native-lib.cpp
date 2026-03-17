#include <jni.h>
#include <string>
#include <vector>
#include <iomanip>
#include <sstream>

// Simple MD5 implementation or use a library if available. 
// For this example, I'll include a compact MD5 implementation directly to avoid external dependencies.

// --- MD5 Implementation Start ---
// Constants for MD5
#define S11 7
#define S12 12
#define S13 17
#define S14 22
#define S21 5
#define S22 9
#define S23 14
#define S24 20
#define S31 4
#define S32 11
#define S33 16
#define S34 23
#define S41 6
#define S42 10
#define S43 15
#define S44 21

static void MD5Transform(unsigned int [4], const unsigned char [64]);

static void MD5Encode(unsigned char *, unsigned int *, unsigned int);

static void MD5Decode(unsigned int *, const unsigned char *, unsigned int);

typedef struct {
    unsigned int state[4];
    unsigned int count[2];
    unsigned char buffer[64];
} MD5_CTX;

static void MD5Init(MD5_CTX *context);

static void MD5Update(MD5_CTX *context, const unsigned char *input, unsigned int inputLen);

static void MD5Final(unsigned char digest[16], MD5_CTX *context);

static unsigned char PADDING[64] = {
        0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

#define F(x, y, z) (((x) & (y)) | ((~x) & (z)))
#define G(x, y, z) (((x) & (z)) | ((y) & (~z)))
#define H(x, y, z) ((x) ^ (y) ^ (z))
#define I(x, y, z) ((y) ^ ((x) | (~z)))

#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32-(n))))

#define FF(a, b, c, d, x, s, ac) { \
 (a) += F ((b), (c), (d)) + (x) + (unsigned int)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define GG(a, b, c, d, x, s, ac) { \
 (a) += G ((b), (c), (d)) + (x) + (unsigned int)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define HH(a, b, c, d, x, s, ac) { \
 (a) += H ((b), (c), (d)) + (x) + (unsigned int)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }
#define II(a, b, c, d, x, s, ac) { \
 (a) += I ((b), (c), (d)) + (x) + (unsigned int)(ac); \
 (a) = ROTATE_LEFT ((a), (s)); \
 (a) += (b); \
  }

static void MD5Init(MD5_CTX *context) {
    context->count[0] = context->count[1] = 0;
    context->state[0] = 0x67452301;
    context->state[1] = 0xefcdab89;
    context->state[2] = 0x98badcfe;
    context->state[3] = 0x10325476;
}

static void MD5Update(MD5_CTX *context, const unsigned char *input, unsigned int inputLen) {
    unsigned int i, index, partLen;

    index = (unsigned int) ((context->count[0] >> 3) & 0x3F);

    if ((context->count[0] += ((unsigned int) inputLen << 3)) < ((unsigned int) inputLen << 3))
        context->count[1]++;
    context->count[1] += ((unsigned int) inputLen >> 29);

    partLen = 64 - index;

    if (inputLen >= partLen) {
        MD5Decode(&context->state[0], &context->buffer[index],
                  partLen); // Fixed: added &context->state[0]
        MD5Transform(context->state, context->buffer);

        for (i = partLen; i + 63 < inputLen; i += 64)
            MD5Transform(context->state, &input[i]);

        index = 0;
    } else
        i = 0;

    MD5Decode(&context->state[0], &context->buffer[index],
              inputLen - i); // Fixed: added &context->state[0]
    // Note: The original MD5 implementation usually uses memcpy here.
    // Replacing MD5Decode calls above with memcpy for simplicity if endianness isn't an issue,
    // but let's stick to a standard structure.
    // Actually, let's simplify. I'll use a very standard compact body.
}

// ... Wait, implementing full MD5 in a single file is verbose. 
// Let's use a simpler approach or just assume standard implementation.
// For the sake of this task, I will implement a clean, standard MD5.

// Re-implementing MD5Update properly with memcpy
#include <cstring>

static void MD5Update_Real(MD5_CTX *context, const unsigned char *input, unsigned int inputLen) {
    unsigned int i, index, partLen;
    index = (unsigned int) ((context->count[0] >> 3) & 0x3F);
    if ((context->count[0] += ((unsigned int) inputLen << 3)) < ((unsigned int) inputLen << 3))
        context->count[1]++;
    context->count[1] += ((unsigned int) inputLen >> 29);
    partLen = 64 - index;

    if (inputLen >= partLen) {
        memcpy((void *) &context->buffer[index], (const void *) input, partLen);
        MD5Transform(context->state, context->buffer);
        for (i = partLen; i + 63 < inputLen; i += 64)
            MD5Transform(context->state, &input[i]);
        index = 0;
    } else {
        i = 0;
    }
    memcpy((void *) &context->buffer[index], (const void *) &input[i], inputLen - i);
}

static void MD5Final(unsigned char digest[16], MD5_CTX *context) {
    unsigned char bits[8];
    unsigned int index, padLen;

    MD5Encode(bits, context->count, 8);

    index = (unsigned int) ((context->count[0] >> 3) & 0x3f);
    padLen = (index < 56) ? (56 - index) : (120 - index);
    MD5Update_Real(context, PADDING, padLen);
    MD5Update_Real(context, bits, 8);
    MD5Encode(digest, context->state, 16);

    memset((void *) context, 0, sizeof(*context));
}

static void MD5Transform(unsigned int state[4], const unsigned char block[64]) {
    unsigned int a = state[0], b = state[1], c = state[2], d = state[3], x[16];
    MD5Decode(x, block, 64);

    FF (a, b, c, d, x[0], S11, 0xd76aa478);
    FF (d, a, b, c, x[1], S12, 0xe8c7b756);
    FF (c, d, a, b, x[2], S13, 0x242070db);
    FF (b, c, d, a, x[3], S14, 0xc1bdceee);
    FF (a, b, c, d, x[4], S11, 0xf57c0faf);
    FF (d, a, b, c, x[5], S12, 0x4787c62a);
    FF (c, d, a, b, x[6], S13, 0xa8304613);
    FF (b, c, d, a, x[7], S14, 0xfd469501);
    FF (a, b, c, d, x[8], S11, 0x698098d8);
    FF (d, a, b, c, x[9], S12, 0x8b44f7af);
    FF (c, d, a, b, x[10], S13, 0xffff5bb1);
    FF (b, c, d, a, x[11], S14, 0x895cd7be);
    FF (a, b, c, d, x[12], S11, 0x6b901122);
    FF (d, a, b, c, x[13], S12, 0xfd987193);
    FF (c, d, a, b, x[14], S13, 0xa679438e);
    FF (b, c, d, a, x[15], S14, 0x49b40821);

    GG (a, b, c, d, x[1], S21, 0xf61e2562);
    GG (d, a, b, c, x[6], S22, 0xc040b340)
    GG (c, d, a, b, x[11], S23, 0x265e5a51);
    GG (b, c, d, a, x[0], S24, 0xe9b6c7aa);
    GG (a, b, c, d, x[5], S21, 0xd62f105d);
    GG (d, a, b, c, x[10], S22, 0x02441453);
    GG (c, d, a, b, x[15], S23, 0xd8a1e681);
    GG (b, c, d, a, x[4], S24, 0xe7d3fbc8);
    GG (a, b, c, d, x[9], S21, 0x21e1cde6);
    GG (d, a, b, c, x[14], S22, 0xc33707d6);
    GG (c, d, a, b, x[3], S23, 0xf4d50d87);
    GG (b, c, d, a, x[8], S24, 0x455a14ed);
    GG (a, b, c, d, x[13], S21, 0xa9e3e905)
    GG (d, a, b, c, x[2], S22, 0xfcefa3f8);
    GG (c, d, a, b, x[7], S23, 0x676f02d9);
    GG (b, c, d, a, x[12], S24, 0x8d2a4c8a);

    HH (a, b, c, d, x[5], S31, 0xfffa3942);
    HH (d, a, b, c, x[8], S32, 0x8771f681);
    HH (c, d, a, b, x[11], S33, 0x6d9d6122);
    HH (b, c, d, a, x[14], S34, 0xfde5380c);
    HH (a, b, c, d, x[1], S31, 0xa4beea44);
    HH (d, a, b, c, x[4], S32, 0x4bdecfa9);
    HH (c, d, a, b, x[7], S33, 0xf6bb4b60);
    HH (b, c, d, a, x[10], S34, 0xbebfbc70);
    HH (a, b, c, d, x[13], S31, 0x289b7ec6);
    HH (d, a, b, c, x[0], S32, 0xeaa127fa);
    HH (c, d, a, b, x[3], S33, 0xd4ef3085);
    HH (b, c, d, a, x[6], S34, 0x04881d05);
    HH (a, b, c, d, x[9], S31, 0xd9d4d039);
    HH (d, a, b, c, x[12], S32, 0xe6db99e5);
    HH (c, d, a, b, x[15], S33, 0x1fa27cf8);
    HH (b, c, d, a, x[2], S34, 0xc4ac5665);

    II (a, b, c, d, x[0], S41, 0xf4292244);
    II (d, a, b, c, x[7], S42, 0x432aff97);
    II (c, d, a, b, x[14], S43, 0xab9423a7);
    II (b, c, d, a, x[5], S44, 0xfc93a039);
    II (a, b, c, d, x[12], S41, 0x655b59c3);
    II (d, a, b, c, x[3], S42, 0x8f0ccc92);
    II (c, d, a, b, x[10], S43, 0xffeff47d);
    II (b, c, d, a, x[1], S44, 0x85845dd1);
    II (a, b, c, d, x[8], S41, 0x6fa87e4f);
    II (d, a, b, c, x[15], S42, 0xfe2ce6e0);
    II (c, d, a, b, x[6], S43, 0xa3014314);
    II (b, c, d, a, x[13], S44, 0x4e0811a1);
    II (a, b, c, d, x[4], S41, 0xf7537e82);
    II (d, a, b, c, x[11], S42, 0xbd3af235);
    II (c, d, a, b, x[2], S43, 0x2ad7d2bb);
    II (b, c, d, a, x[9], S44, 0xeb86d391);

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;

    memset((void *) x, 0, sizeof(x));
}

static void MD5Encode(unsigned char *output, unsigned int *input, unsigned int len) {
    unsigned int i, j;

    for (i = 0, j = 0; j < len; i++, j += 4) {
        output[j] = (unsigned char) (input[i] & 0xff);
        output[j + 1] = (unsigned char) ((input[i] >> 8) & 0xff);
        output[j + 2] = (unsigned char) ((input[i] >> 16) & 0xff);
        output[j + 3] = (unsigned char) ((input[i] >> 24) & 0xff);
    }
}

static void MD5Decode(unsigned int *output, const unsigned char *input, unsigned int len) {
    unsigned int i, j;

    for (i = 0, j = 0; j < len; i++, j += 4)
        output[i] = ((unsigned int) input[j]) | (((unsigned int) input[j + 1]) << 8) |
                    (((unsigned int) input[j + 2]) << 16) | (((unsigned int) input[j + 3]) << 24);
}
// --- MD5 Implementation End ---

std::string md5(const std::string &str) {
    MD5_CTX ctx;
    MD5Init(&ctx);
    MD5Update_Real(&ctx, (unsigned char *) str.c_str(), str.length());
    unsigned char digest[16];
    MD5Final(digest, &ctx);

    std::stringstream ss;
    for (int i = 0; i < 16; ++i) {
        ss << std::hex << std::setw(2) << std::setfill('0') << (int) digest[i];
    }
    return ss.str();
}

#include <sys/ptrace.h>
#include <unistd.h>
#include <fstream>
#include <thread>
#include <chrono>
#include <android/log.h>

#define LOG_TAG "NativeSecurity"
// Disable logging in release for security
#ifdef NDEBUG
#define LOGE(...)
#else
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif

// --- XOR Encryption ---
// Simple XOR key
const char XOR_KEY = 0x42;

std::string decrypt(const std::string& encrypted) {
    std::string decrypted = encrypted;
    for (size_t i = 0; i < encrypted.size(); i++) {
        decrypted[i] = encrypted[i] ^ XOR_KEY;
    }
    return decrypted;
}

// Encrypted "MusicKillerNativeSalt" -> XOR with 0x42
// M(0x4D)^0x42=0x0F, u(0x75)^0x42=0x37, etc.
// To generate this, you can run a small script. 
// For now, I will use a dynamic generation approach to avoid hardcoding the encrypted string in a way that's easily readable in this prompt,
// but in a real scenario, you'd have the byte array ready.
// Let's just use a runtime obfuscation for the salt to demonstrate the concept without pre-calculating bytes here.
std::string getSalt() {
    // "MusicKillerNativeSalt"
    // We construct it char by char or use a byte array.
    // Byte array is better.
    // M u s i c K i l l e r N a t i v e S a l t
    char salt[] = { 
        (char)('M' ^ XOR_KEY), (char)('u' ^ XOR_KEY), (char)('s' ^ XOR_KEY), (char)('i' ^ XOR_KEY), (char)('c' ^ XOR_KEY),
        (char)('K' ^ XOR_KEY), (char)('i' ^ XOR_KEY), (char)('l' ^ XOR_KEY), (char)('l' ^ XOR_KEY), (char)('e' ^ XOR_KEY), (char)('r' ^ XOR_KEY),
        (char)('N' ^ XOR_KEY), (char)('a' ^ XOR_KEY), (char)('t' ^ XOR_KEY), (char)('i' ^ XOR_KEY), (char)('v' ^ XOR_KEY), (char)('e' ^ XOR_KEY),
        (char)('S' ^ XOR_KEY), (char)('a' ^ XOR_KEY), (char)('l' ^ XOR_KEY), (char)('t' ^ XOR_KEY), 0 
    };
    // checkDebug(); // Careful, ptrace might interfere with Android Studio debugging if not handled
    // checkHook();
    return decrypt(salt);
}

#include <zlib.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <sys/syscall.h>

// --- Security Checks ---

void checkDebug() {
    if (ptrace(PTRACE_TRACEME, 0, 0, 0) == -1) {
        LOGE("Debugger detected via ptrace");
        exit(0);
    }
    std::ifstream status("/proc/self/status");
    std::string line;
    while (std::getline(status, line)) {
        if (line.find("TracerPid:") != std::string::npos) {
            int pid = std::stoi(line.substr(10));
            if (pid != 0) {
                LOGE("Debugger detected via TracerPid");
                exit(0);
            }
            break;
        }
    }
}

void checkHook() {
    std::ifstream maps("/proc/self/maps");
    std::string line;
    while (std::getline(maps, line)) {
        if (line.find("frida") != std::string::npos || 
            line.find("xposed") != std::string::npos || 
            line.find("com.saurik.substrate") != std::string::npos) {
            LOGE("Hook framework detected in maps");
            exit(0);
        }
    }
}

// --- Syscall Helpers (Anti-Libc Hook) ---

int sys_open(const char *pathname, int flags) {
    return syscall(__NR_openat, -100, pathname, flags, 0);
}

ssize_t sys_read(int fd, void *buf, size_t count) {
    return syscall(__NR_read, fd, buf, count);
}

off_t sys_lseek(int fd, off_t offset, int whence) {
    return syscall(__NR_lseek, fd, offset, whence);
}

int sys_close(int fd) {
    return syscall(__NR_close, fd);
}

// --- App Signature Verification ---

std::string getApkPath(JNIEnv *env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getApplicationInfo = env->GetMethodID(contextClass, "getApplicationInfo", "()Landroid/content/pm/ApplicationInfo;");
    jobject applicationInfo = env->CallObjectMethod(context, getApplicationInfo);
    jclass applicationInfoClass = env->GetObjectClass(applicationInfo);
    jfieldID sourceDirField = env->GetFieldID(applicationInfoClass, "sourceDir", "Ljava/lang/String;");
    jstring sourceDir = (jstring)env->GetObjectField(applicationInfo, sourceDirField);

    const char *path = env->GetStringUTFChars(sourceDir, 0);
    std::string apkPath(path);
    env->ReleaseStringUTFChars(sourceDir, path);
    return apkPath;
}

std::string getSignatureBytes(JNIEnv *env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPackageManager = env->GetMethodID(contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    jobject packageManager = env->CallObjectMethod(context, getPackageManager);

    jmethodID getPackageName = env->GetMethodID(contextClass, "getPackageName", "()Ljava/lang/String;");
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageName);

    jclass packageManagerClass = env->GetObjectClass(packageManager);
    jmethodID getPackageInfo = env->GetMethodID(packageManagerClass, "getPackageInfo", "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    
    jobject packageInfo = env->CallObjectMethod(packageManager, getPackageInfo, packageName, 64);
    
    jclass packageInfoClass = env->GetObjectClass(packageInfo);
    jfieldID signaturesField = env->GetFieldID(packageInfoClass, "signatures", "[Landroid/content/pm/Signature;");
    jobjectArray signatures = (jobjectArray)env->GetObjectField(packageInfo, signaturesField);
    
    jobject signature = env->GetObjectArrayElement(signatures, 0);
    jclass signatureClass = env->GetObjectClass(signature);
    jmethodID toByteArray = env->GetMethodID(signatureClass, "toByteArray", "()[B");
    jbyteArray signatureBytes = (jbyteArray)env->CallObjectMethod(signature, toByteArray);
    
    jsize length = env->GetArrayLength(signatureBytes);
    jbyte* bytes = env->GetByteArrayElements(signatureBytes, NULL);
    
    std::string signatureString((char*)bytes, length);
    
    env->ReleaseByteArrayElements(signatureBytes, bytes, 0);
    
    return signatureString;
}

std::pair<std::string, unsigned int> scanApk(const std::string& apkPath) {
    int fd = sys_open(apkPath.c_str(), O_RDONLY);
    if (fd < 0) return {"", 0};

    off_t fileSize = sys_lseek(fd, 0, SEEK_END);
    long searchLimit = fileSize - 65536;
    if (searchLimit < 0) searchLimit = 0;
    
    long eocdOffset = -1;
    unsigned char buf[4];
    for (long i = fileSize - 22; i >= searchLimit; i--) {
        sys_lseek(fd, i, SEEK_SET);
        sys_read(fd, buf, 4);
        if (buf[0] == 0x50 && buf[1] == 0x4b && buf[2] == 0x05 && buf[3] == 0x06) {
            eocdOffset = i;
            break;
        }
    }

    if (eocdOffset == -1) {
        sys_close(fd);
        return {"", 0};
    }

    sys_lseek(fd, eocdOffset + 16, SEEK_SET);
    unsigned int cdOffset;
    sys_read(fd, &cdOffset, 4);
    
    sys_lseek(fd, eocdOffset + 12, SEEK_SET);
    unsigned int cdSize;
    sys_read(fd, &cdSize, 4);

    sys_lseek(fd, cdOffset, SEEK_SET);
    long currentPos = cdOffset;
    long endPos = cdOffset + cdSize;

    std::string certContent = "";
    unsigned int dexCrc = 0;

    while (currentPos < endPos) {
        sys_lseek(fd, currentPos, SEEK_SET);
        unsigned char header[46];
        if (sys_read(fd, header, 46) != 46) break;

        if (header[0] != 0x50 || header[1] != 0x4b || header[2] != 0x01 || header[3] != 0x02) break;

        unsigned short method = header[10] | (header[11] << 8);
        unsigned int crc32 = header[16] | (header[17] << 8) | (header[18] << 16) | (header[19] << 24);
        unsigned int compressedSize = header[20] | (header[21] << 8) | (header[22] << 16) | (header[23] << 24);
        unsigned int uncompressedSize = header[24] | (header[25] << 8) | (header[26] << 16) | (header[27] << 24);
        unsigned short fileNameLen = header[28] | (header[29] << 8);
        unsigned short extraLen = header[30] | (header[31] << 8);
        unsigned short commentLen = header[32] | (header[33] << 8);
        unsigned int localHeaderOffset = header[42] | (header[43] << 8) | (header[44] << 16) | (header[45] << 24);

        char* fileName = new char[fileNameLen + 1];
        sys_read(fd, fileName, fileNameLen);
        fileName[fileNameLen] = 0;
        std::string name(fileName);
        delete[] fileName;

        if (name == "classes.dex") {
            dexCrc = crc32;
            LOGE("Found classes.dex CRC32: %08x", dexCrc);
        }

        if (certContent.empty() && name.find("META-INF/") == 0) {
            if (name.find(".RSA") != std::string::npos || name.find(".EC") != std::string::npos || name.find(".DSA") != std::string::npos) {
                sys_lseek(fd, localHeaderOffset, SEEK_SET);
                unsigned char localHeader[30];
                sys_read(fd, localHeader, 30);
                
                unsigned short lFileNameLen = localHeader[26] | (localHeader[27] << 8);
                unsigned short lExtraLen = localHeader[28] | (localHeader[29] << 8);
                
                sys_lseek(fd, localHeaderOffset + 30 + lFileNameLen + lExtraLen, SEEK_SET);
                
                unsigned char* compressedData = new unsigned char[compressedSize];
                sys_read(fd, compressedData, compressedSize);
                
                if (method == 0) {
                    certContent.assign((char*)compressedData, compressedSize);
                } else if (method == 8) {
                    unsigned char* uncompressedData = new unsigned char[uncompressedSize];
                    z_stream strm;
                    strm.zalloc = Z_NULL;
                    strm.zfree = Z_NULL;
                    strm.opaque = Z_NULL;
                    strm.avail_in = compressedSize;
                    strm.next_in = compressedData;
                    strm.avail_out = uncompressedSize;
                    strm.next_out = uncompressedData;
                    
                    inflateInit2(&strm, -MAX_WBITS);
                    inflate(&strm, Z_FINISH);
                    inflateEnd(&strm);
                    
                    certContent.assign((char*)uncompressedData, uncompressedSize);
                    delete[] uncompressedData;
                }
                delete[] compressedData;
            }
        }
        currentPos += 46 + fileNameLen + extraLen + commentLen;
    }
    sys_close(fd);
    return {certContent, dexCrc};
}

void checkAppSignature(JNIEnv *env, jobject context) {
    std::string jniCert = getSignatureBytes(env, context);
    std::string jniCertHash = md5(jniCert);
    std::string expectedHash = "45af602790ec8031540c99bde7168827"; 
    
    LOGE("JNI Cert Hash: %s", jniCertHash.c_str());
    if (jniCertHash != expectedHash) {
        exit(0);
    }

    std::string apkPath = getApkPath(env, context);
    if (apkPath.empty()) return;
    
    auto result = scanApk(apkPath);
    std::string certFileContent = result.first;
    unsigned int dexCrc = result.second;

    if (certFileContent.empty()) {
        LOGE("Failed to find signature in APK (Syscall)");
        exit(0);
    }
    if (certFileContent.find(jniCert) == std::string::npos) {
        LOGE("Hook detected! JNI Cert not found in APK file (Syscall).");
        exit(0);
    }

    LOGE("Dex CRC: %08x", dexCrc);
    unsigned int expectedDexCrc = 0x07306648; // REPLACE THIS
    if (dexCrc != expectedDexCrc) {
        LOGE("Dex CRC mismatch! Expected: %08x, Got: %08x", expectedDexCrc, dexCrc);
        // exit(0);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_xyz_jdynb_music_MusicKillerApplication_getSign(
        JNIEnv *env,
        jobject thiz,
        jstring timestamp,
        jstring version) {

    #ifdef NDEBUG
    // checkDebug();
    #endif
    // checkHook();
    // checkAppSignature(env, thiz);

    const char *nativeTimestamp = env->GetStringUTFChars(timestamp, 0);
    const char *nativeVersion = env->GetStringUTFChars(version, 0);

    std::string salt = getSalt();
    std::string content = salt + nativeTimestamp + nativeVersion;

    std::string signature = md5(content);

    env->ReleaseStringUTFChars(timestamp, nativeTimestamp);
    env->ReleaseStringUTFChars(version, nativeVersion);

    return env->NewStringUTF(signature.c_str());
}
