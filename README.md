# HelloAI for Android

**HelloAI**, TECHGPTX markası altında geliştirilen, sesli etkileşim ve cihaz otomasyonu özelliklerini tek bir yardımcı deneyiminde birleştiren Android asistanıdır.

Geliştirici: **Muhammed**

Resmî proje deposu: [github.com/ThT0AltayHR/cocoumy](https://github.com/ThT0AltayHR/cocoumy)

## Öne çıkan özellikler

- Gemini Live tabanlı sesli sohbet altyapısı
- Türkçe başlangıç tercihleri ve isteğe bağlı “Hey Merhaba” uyandırma sözcüğü
- Mikrofon, kamera ve hoparlör/ahize ses yolu yönetimi
- Görünür canlı kamera ekranından Gemini Live’a görüntü aktarımı
- Dinleme, düşünme ve yanıt durumlarını gösteren üst katman arayüzü
- Kişi/arama, medya ve erişilebilirlik tabanlı cihaz otomasyonu
- Sohbet geçmişi, hafıza, widget ve tema ayarları
- Koyu ve açık arayüz seçenekleri

## Uygulama bilgileri

- Uygulama kimliği: `com.myappai.app`
- Android modülü: `app`
- Minimum SDK: Proje ayarlarında tanımlıdır
- Hedef SDK: Android 35

## Gereksinimler

- Android Studio veya JDK 17
- Android SDK Platform 35 ve Build Tools 35.0.0
- Gemini API anahtarı
- Gradle Wrapper (`./gradlew`)

## Yerel derleme

```bash
chmod +x ./gradlew
bash tools/fetch_turkish_wake_model.sh
./gradlew --no-daemon :app:assembleDebug
```

Başarılı derlemeden sonra APK şu konumda oluşur:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Türkçe ses modeli indirilemezse derleme yine yapılabilir; yalnızca Türkçe uyandırma sözcüğü özelliği kullanılamaz.

## GitHub Actions

`.github/workflows/android-release.yml`, `main` veya `master` dalına gönderilen değişikliklerde ve manuel çalıştırmalarda Android APK derler. Üretilen debug APK, workflow tamamlandığında **Actions → workflow run → Artifacts** bölümünden indirilebilir.

Release imzalama bilgileri GitHub Actions secrets olarak yapılandırılırsa workflow imzalı release APK da üretir. Keystore dosyasını veya API anahtarını kaynak koda koymayın.

## İlk kullanım

1. APK’yı Android cihazınıza yükleyin.
2. Uygulama içinden Gemini API anahtarını yapılandırın.
3. Mikrofon, kamera ve bildirim izinlerini gerektiğinde verin.
4. Üst katman arayüzü için “Diğer uygulamaların üzerinde göster” iznini etkinleştirin.
5. Cihaz otomasyonu gerekiyorsa erişilebilirlik hizmetini Android ayarlarından etkinleştirin.

## Güvenlik ve gizlilik

API anahtarlarını GitHub’a, APK’ya veya kaynak dosyalarına sabitlemeyin. Kamera aktarımı yalnızca kullanıcı tarafından canlı kamera ekranı açıldığında yapılmalıdır. Erişilebilirlik, mikrofon ve kamera izinlerini yalnızca ihtiyaç duyduğunuz özellikler için etkinleştirin.

## Lisans ve kaynaklar

Bu proje, Android port kaynaklarını ve gerekli yardımcı dosyaları içerir. Dağıtım veya ticari kullanım öncesinde kullanılan özgün kaynakların lisanslarını ve üçüncü taraf bildirimlerini inceleyin. Ayrıntılar için `ATTRIBUTION_AND_LICENSES.md` dosyasına bakın.

## TECHGPTX iletişim kanalları

Uygulama içindeki YouTube, Instagram ve Telegram düğmeleri TECHGPTX topluluk bağlantılarına yönlendirir. Marka ve geliştirici kredisi: **TECHGPTX — Muhammed**.
