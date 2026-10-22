import 'package:encrypt/encrypt.dart';
import 'package:pointycastle/asymmetric/api.dart';

/// Par de llaves RSA de 1024 bits SOLO para tests (generadas con openssl): la pública se entrega
/// como la entrega el backend en /v1/auth/public-key (base64 del X.509, sin cabeceras PEM).
const testRsaPublicKeyBase64 =
    'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3U8peGg30yhLUIEPOx6AYOdZGxOz/sv9k6VjB9JByAvjrvwHHrg3wVG01aNuP9KY2TJ8dMoGtXv4wRB0gvHusg4Wm/HsDdb7yLEXdKzMa40Ab8ynYPaQWoT7chR7giNRu8us15MPTCzjsTRHq7sN2UtEBuVGziJgvhC3Bo37P1wIDAQAB';

const _testRsaPrivateKeyPem = '''-----BEGIN RSA PRIVATE KEY-----
MIICXgIBAAKBgQC3U8peGg30yhLUIEPOx6AYOdZGxOz/sv9k6VjB9JByAvjrvwHH
rg3wVG01aNuP9KY2TJ8dMoGtXv4wRB0gvHusg4Wm/HsDdb7yLEXdKzMa40Ab8ynY
PaQWoT7chR7giNRu8us15MPTCzjsTRHq7sN2UtEBuVGziJgvhC3Bo37P1wIDAQAB
AoGAUDF7CIblK/tpGRm0D7QLOA20yehCs+edCIt6o/1TYkwijx1SrgJZXpPP4dko
kmQ2gPIreB4r06cGEh75juRkRGfV5t/9fXVwJnITVJ7fB1EmzSbPLVoxHiY+ZVuX
6QdddeZ2a09rKnlG6QWVcy/fzegXMrXsIH+sZURyA7ujSCECQQDyja9djrUMIbpJ
mHp1of9Da0IgTiwcJ+0p9432PZosUVPRr0gqNtOXfeLZja5kzf04QryZN7igbOcp
JBqmn1I9AkEAwX2RoBk6a1maTtHCas89acTva2BeNwBtaOD5h0vR4ybLzEpzVozo
1ntmftprkcEzKpV11iMjahJzCFqgVElvowJBAKJPzhp9dQo5R8AYGVq+nWMwU3/q
z/nHfh2nW4h8kL83bS/UbCZTqiSmSTfsZ9dtQRZ1EmA1uYchWU9eqbb2xsECQQCI
kwyeWBBfQ7Din7a+U6fCp6pjPXNjGV8g32PiNwdgcbr6aTa+SdnzwsiXE69IA3oN
PdAeJVFTBtDBfc6bj/ZHAkEA7RiS1Lza/5X2Jn8aKJSU9FpJRL5Xd3CG6tEjDNR2
NS3Uehsssevkw6rAovEJ+LblLVSsJtVFiQrqkuGcmx+VcQ==
-----END RSA PRIVATE KEY-----''';

/// Descifra lo que la app cifró con la llave pública, como haría el backend.
String decryptWithTestPrivateKey(String base64CipherText) {
  final privateKey = RSAKeyParser().parse(_testRsaPrivateKeyPem) as RSAPrivateKey;
  return Encrypter(RSA(privateKey: privateKey)).decrypt(Encrypted.fromBase64(base64CipherText));
}
