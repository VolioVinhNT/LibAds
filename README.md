# libads
# Setup
1. Clone Modul quảng cáo về cùng thư mục với App
2. Thêm modul vào Project
  - file Setting.gradle thêm
  
    include ':Ads'
    project(':Ads').projectDir = new File(settingsDir, '../LibAds/Ads')
  
  
  - file build.gradle thêm
  
    implementation project(path: ':Ads')

3. File AndroidManifest
 Thêm:
 
<meta-data
     android:name="com.google.android.gms.ads.APPLICATION_ID"
     android:value="@string/admob_app_id" />
     
     (admob_app_id xin bên PO)

# Init
Phải gọi init đầu tiên trước khi gọi những hàm khác
