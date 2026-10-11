# Proje durumu — Kolay Hatlı Kur'an

## Bu pakette çalışan olarak tasarlanan özellikler
- 1–604 sayfa numaraları üzerinden ayetleri görüntüleme
- Arapça Uthmani metin ve API mevcutsa Türkçe Diyanet meali
- Son sayfa ve tek yer imini cihazda saklama
- Ayet numarasından Mishary Rashid Alafasy sesini internet üzerinden çalma
- Başlangıç düzeyi harf/tecvid çalışma notları
- Arapça sayfa verileri ve API'nin sağladığı meal verilerini cihaz önbelleğine indirme
- GitHub Actions ile debug APK üretimi

## Bilinen kapsam sınırları
- Bu uygulama basılı Medine mushafının taranmış sayfalarını göstermez. Sayfalar, 604 mushaf sayfa numarasına göre API'den gelen ayetleri metin olarak sunar; satır/sayfa dizilimi basılı mushafla birebir aynı değildir.
- Sesler topluca indirilmez; sesli okuma internet gerektirir.
- Ayet senkronizasyonu/zaman damgalı kelime vurgusu yoktur; seçilen ayetin sesini çalma vardır.
- Meal API erişilebilirliğine ve ilgili edition'ın kullanılabilirliğine bağlıdır.
- Bu ortamda Android SDK ve Gradle ile derleme yapılmadığından APK'nın derlendiği veya gerçek cihazda test edildiği iddia edilmez. GitHub Actions derlemesi doğrulanmalıdır.

## İlk derleme sonrası kabul kontrolü
1. GitHub Actions iş akışı yeşil tamamlanmalı.
2. `app-debug.apk` Samsung Galaxy A17 5G'ye kurulmalı.
3. İnternet varken sayfa 1, orta sayfa ve 604 açılmalı; Arapça ve meal karşılaştırılmalı.
4. Yer imi ve son sayfa uygulama kapatılıp açılınca korunmalı.
5. 604 sayfalık indirme tamamlanmalı; uçak modunda önceden indirilen sayfalar açılmalı.
6. Sesin internet gerektirdiği kullanıcıya açık olmalı.

## İçerik güvenliği
Kur'an metni ve meal uygulama tarafından üretilmez. Uygulama metin kaynağını değiştirmeden gösterir. Yayın öncesi metin, meal, ses kaynaklarının doğruluğu ve yeniden kullanım koşulları kontrol edilmelidir.
