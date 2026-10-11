# Kolay Hatlı Kur'an — Android uygulaması

Kotlin + Jetpack Compose ile Android 8.0+ hedeflenerek hazırlanmış, GitHub Actions üzerinden APK derlenebilen bir proje.

## Dahil olanlar
- Sayfa numarasına göre Kur'an metnini yükleme (1–604).
- Arapça metin ve Türkçe Diyanet meali (API kaynağı sağladığı ölçüde).
- Ziyaret edilen sayfaları cihaz dosyalarına önbelleğe alma.
- Son okunan sayfayı ve yer imini kalıcı kaydetme.
- Ayet numarasından Mishary Rashid Alafasy ses akışı.
- Başlangıç düzeyi tecvid/harf çalışma notları.
- 604 sayfanın Arapça metin ve meal verilerini çevrimdışı kullanım için indirme ekranı.
- GitHub Actions APK build workflow.

## Önemli içerik notu
Bu paket **basılı Medine mushafının özgün sayfa taramalarını içermez**. Mushaf sayfa numaralarına göre ayet metinlerini gösterir; font ve satır akışı basılı sayfanın birebir kopyası değildir. Bunun için hakları/izinleri doğrulanmış 604 sayfalık görüntü paketi ayrıca eklenmelidir. Meal ve ses de ilk çalıştırmada çevrimiçi kaynaktan yüklenir; tüm sayfaları indirme işlemi sonrasında Arapça metin ve API'nin sağladığı meal cihazda önbelleğe alınır. Sesler topluca çevrimdışı indirilmez.

Kur'an metni kaynağı: Tanzil Project (Uthmani). Tanzil lisansı metnin değiştirilmeden çoğaltılmasını, kaynak bildirimini ve tanzil.net bağlantısını gerektirir: https://tanzil.net/docs/Text_License

Veri API'si: https://alquran.cloud/api — sayfa metinleri `quran-uthmani` ve `tr.diyanet` edition uç noktalarından alınır. Ses CDN'i: https://cdn.islamic.network/quran/audio/128/ar.alafasy/

## GitHub'dan APK üretme
1. Bu klasörün içeriğini GitHub'da `kolay-hatli-kuran` adlı depoya yükle. ZIP dosyasını doğrudan depoya koymak yeterli değildir; ZIP'i çıkarıp içindeki dosyaları yükle.
2. `Actions` sekmesine gir.
3. `Build Android APK` workflow'unu seç ve `Run workflow` düğmesine bas.
4. İşlem tamamlanınca `kolay-hatli-kuran-debug-apk` artifact'ini indirip aç.
5. İçindeki `app-debug.apk` dosyasını Android telefonuna aktar ve kur.

## Test durumu
Bu çalışma ortamında internet erişimi ve Android SDK/Gradle kurulu olmadığı için APK burada derlenemedi veya cihazda test edilemedi. İlk GitHub Actions derlemesinin sonucu doğrulanmalıdır. API erişimi ve edition kullanılabilirliği de ilk çalıştırmada test edilmelidir.


### GitHub Actions SDK lisansları

İş akışı, derlemeden önce `sdkmanager --licenses` komutuyla Android SDK lisanslarını otomatik kabul etmeyi dener.
