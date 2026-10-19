# Port durum raporu

Bu belge, bu kaynak ZIP'inin gerçekte ne içerdiğini ve tamamlanması gerekenleri ayırır.

| Alan | Bu paketteki durum | Doğrulama |
|---|---|---|
| macOS Swift kaynaklarının Android'e taşınması | Android Kotlin tabanlı mevcut asistan kullanıldı; macOS kaynak arşivi referans olarak eklendi | Tam özellik eşitliği sağlanmış değildir |
| Gemini Live ses sohbeti | Sağlanan Android projesindeki WebSocket/audio altyapısı korunup Gemini varsayılanları Türkçe yapıldı | API anahtarı ve gerçek ağ oturumu ile cihaz testi gerekir |
| Çok modlu kamera | CameraX ön/arka kamera ekranı ve kare gönderme eklendi | API ile görüntü akışı test edilmeli |
| Dinamik çentik benzeri overlay | `TYPE_APPLICATION_OVERLAY` UI eklendi | Kullanıcı overlay izni ve farklı Android/OEM testleri gerekir |
| Ses çıkışı | Hoparlör/ahize yönlendirme denetleyicisi eklendi | Cihaz bazlı test gerekir; Bluetooth çıkışı ayrıca test edilmedi |
| Ana ekran widget'ı | Durum ve sohbet açma widget'ı eklendi | Launcher/device test gerekir |
| Türkçe UI/prompt | Uygulama adı ve birçok görünür ekran/istem metni Türkçeleştirildi | Tüm eski ekranlardaki her metnin yüzde 100 Türkçe olduğu ileri sürülmüyor; UI turu gerekir |
| Uyandırma sözcüğü | `Hey Merhaba` kod yolu eklendi; CI Türkçe Vosk modelini getiriyor | Canlı model dosyası bu ZIP'e gömülü değil; cihaz ve gürültü testleri gerekir |
| Kişi/arama/WhatsApp/uygulama otomasyonu | Sağlanan Android projesinin mevcut entegrasyonlarına dayanır | İzinler, uygulama sürümü ve eylem doğrulaması gerekir |
| macOS özel özellikler/agent entegrasyonları | Bütün 186 Swift dosyası birebir Android'e port edilmedi | Özellik özellik fark analizi ve ek implementasyon gerekir |
| Release APK | GitHub Actions workflow'u hazır; imza sırları şartlı | Bu sandbox oturumunda derleme doğrulanamadı |

## Release öncesi kontrol listesi

- [ ] GitHub Actions debug derlemesini çalıştırıp ilk hata loglarını düzelt.
- [ ] Türkçe Vosk model indirme adımının başarılı olduğunu doğrula.
- [ ] Gerçek Gemini API anahtarıyla metin, mikrofon, ses çıkışı ve kamera görüntüsünü test et.
- [ ] Android 13/14/15+ üzerinde izin, arka plan kısıtları, pil tüketimi ve overlay davranışını test et.
- [ ] Kalıcı release keystore ve gerçek yayıncı bilgilerini GitHub Secrets'a koy.
- [ ] İmzalı APK'yı `apksigner verify --verbose --print-certs` ile doğrula ve gerçek cihazda kurulum/güncelleme testi yap.
- [ ] Orijinal projelerin lisanslarını/dağıtım hakkını doğrula.
