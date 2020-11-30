# libads
## Setup
1. Clone Modul quảng cáo về cùng thư mục với App
2. Thêm modul vào Project
  - file Setting.gradle thêm
  
    include ':Ads'
    project(':Ads').projectDir = new File(settingsDir, '../LibAds/Ads')
  
  
  - file build.gradle thêm
  
    implementation project(path: ':Ads')

3. File AndroidManifest
 Thêm:
     ```xml
     <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="@string/admob_app_id" />
     ```

     (admob_app_id xin bên PO)

## Init
Phải gọi init đầu tiên trước khi gọi những hàm khác

AdsController.initinit(
            activity: Activity,
            isDebug:Boolean,
            listAppId: ArrayList<String>,
            packetName: String,
            listPathJson: ArrayList<String>, lifecycle: Lifecycle
        )
|Parameters| Giá trị|
|------|-------|
|activity| Activity|
|isDebug| Giá trị bằng true sẽ sử dụng id quảng cáo test, bằng false <br>sử dụng id thật chỉ sử dụng khi build release |
|listAppId| Là một list String id của mạng quảng cáo được thêm trong AndroidManifest admob_app_id |
|packetName| Là packet của app lấy trong file build.gradle (modul app) => applicationId  |
|listPathJson| Là list path json của mạng quảng cáo (file json nằm trong assets)<br>vd: arrayListOf("admod_json/admod_id.json")|
|lifecycle | Là lifecycle của activity|

## Sử dụng
