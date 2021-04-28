package jp.co.uv.blind.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import org.mozilla.universalchardet.UniversalDetector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.uv.blind.common.Def;
import jp.co.uv.blind.common.LanguageSupport;
import jp.co.uv.blind.model.Direction;
import jp.co.uv.blind.model.Facility;
import jp.co.uv.blind.model.Information;
import jp.co.uv.blind.util.JapanesePattern;

public class Decode {
    // uvtb_type attribute bit
    private final static int UVTB_TYPE_MASK = 0x0F;
    private final static int UVTB_TYPE_ATTR_MASK = 0xF0;
    private final static int UVTB_TYPE_ATTR_NO_SPEAK = 0x80;
    private final static int UVTB_TYPE_ATTR_NO_SAVE = 0x40;

    private Facility facility;
    private Information information;
    private ArrayList<Direction> directions;
    private ArrayList<Facility> facilities;
    private String deviceLanguage;
    private boolean isNavi = false;
    private boolean isFacility = false;
    private boolean isVoiceCode = true;

    private byte[] data;
    private int width;
    private int height;
    private Context context;

    protected byte[] contentByte;
    private String codeID;
    private String companyID;
    private String projectID;
    private boolean hasGotoResult = false;

    private int invalid;
    private int brightnessMin;
    private int brightnessMax;
    private int bufferType;

    private String strId;
    private int statusCode;
    private String currentLanguage = ".jpn";

    private String savedContent;
    private String speakContent;
    private String savedSpeakContent;
    private String content;
    private boolean isJapanese;
    private int countRun = 0;
    public Map<String, String> mLanguageMap = null;

    public Decode(Context ctx, byte[] data, int width, int height) {
        this.context = ctx;
        this.data = data;
        this.width = width;
        this.height = height;

        if (mLanguageMap == null) {
            mLanguageMap = new LinkedHashMap<>();
        }
    }

    public native void setUvDecodeSymMode(int mode);

    public native int getUvDecodeSymMode();

    public native int callJavis(int width, int height, byte[] bytes, int threshold, AssetManager manager);

    static {
        System.loadLibrary("uv_camera");
    }

    public void setSuggestedWord(String id, int code, byte[] c, int invalid, int bufferType) {
        if (c != null && id != null) {
            contentByte = Arrays.copyOfRange(c, 0, c.length - id.getBytes().length);
        } else {
            contentByte = c;
        }
        this.strId = id;
        this.statusCode=code;
        this.invalid=invalid;
        this.bufferType=bufferType;

        handleDecode();
    }

    public void setDeviceLanguage(String language) {
        this.deviceLanguage = language;
    }


    private void handleDecode() {
        if (invalid == -45) {
            //TODO: DETECT INVALID CODE
        }
        if (bufferType == 3) {
            handleBufferType3();

        } else if (bufferType == 4) { // New JAVIS
            handleBufferType4();
        }
    }

    private void handleBufferType3() {
        try {
            String deString = new String(contentByte, Def.STRING_FORMAT_JAPANESE);
            if (deString.contains("�")) {
                deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
            }
            if (deString.contains("<tag")) {
                savedContent = deString;
                speakContent = deString;
                savedSpeakContent = deString;
                content = deString;
            } else {
                if (!isDigitOnly(deString)) {
                    if (JapanesePattern.isJapanese(deString)) {
                        savedContent = deString;
                        speakContent =deString;
                        savedSpeakContent = deString;
                        content = deString;
                        isJapanese=true;
                    } else {
                        detectNoTagCode();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!hasGotoResult) {
//            gotoResultScreen();
        }

    }

    private void handleBufferType4() {
        int i = 0;
        String deString = "";
        String strSave = "";
        String strSpeak = "";
        String strSavedSpeak = "";
        String strCodeID = "";      // code ID

        while (i < contentByte.length - 1) {
            int nType = 0;
            String sCountry = "";
            String content = "";
            int nAttr = 0;
            int nLength = 0;

            boolean isSpeak = true;
            boolean isSave = true;

            // Attribute
            nAttr = (int) (contentByte[i] & UVTB_TYPE_ATTR_MASK);
            // DataType
//                    nType = (int) contentByte[i++];
            nType = (int) (contentByte[i++] & UVTB_TYPE_MASK);
            Log.i("grape", "type = " + nType);
            if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                    == UVTB_TYPE_ATTR_NO_SPEAK) {
                Log.i("grape", "attr = no_speak");
                isSpeak = false;
            } else if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                    == UVTB_TYPE_ATTR_NO_SAVE) {
                Log.i("grape", "attr = no_save");
                isSave = false;
            } else if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                    == (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE)) {
                Log.i("grape", "attr = no_speak, no_save");
                isSpeak = false;
                isSave = false;
            }
            // CountryCode
            sCountry = new String(new byte[]{contentByte[i++]});
            sCountry += new String(new byte[]{contentByte[i++]});
            sCountry += new String(new byte[]{contentByte[i++]});

            // Length
            int sByte1 = contentByte[i++];
            int sByte2 = contentByte[i++];
            if (sByte1 < 0) {
                sByte1 += 256;
            }
            if (sByte2 < 0) {
                sByte2 += 256;
            }
            nLength = sByte1;
            nLength += sByte2 * 256;

            // TextData
            byte[] byteText = new byte[nLength];
            for (int j = 0; j < nLength; j++) {
                byteText[j] = contentByte[i];
                i++;
            }

            // Convert
            try {
                // Japanese
                if (nType == 1 || nType == 4) { // SJIS / Ascii(ISO/IEC-8859-1
                    if (currentLanguage.equalsIgnoreCase("." + sCountry)) {
                        String contentString = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                        deString = contentString + "\n\n" + deString;
                        Log.d("Scan Activity ", "deString " + deString);
                        if (isSpeak) {
                            strSpeak = contentString + "\n\n" + strSpeak;
                            Log.d("Scan Activity ", "strSpeak " + strSpeak);
                        }
                    } else {
                        deString += new String(byteText, Def.STRING_FORMAT_JAPANESE) + "\n\n";
                    }
                    content = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                } else if (nType == 2) { // UTF-8 // English
                    if (currentLanguage.equalsIgnoreCase("." + sCountry)) {
                        String contentString = new String(byteText, Def.SERVER_STRING_FORMAT);
                        deString = contentString + "\n\n" + deString;
                        if (isSpeak) {
                            strSpeak = contentString + "\n\n" + strSpeak;
                        }
                    } else {
                        deString += new String(byteText, Def.SERVER_STRING_FORMAT) + "\n\n";
                    }
                    content = new String(byteText, Def.SERVER_STRING_FORMAT);
                } else if (nType == 3) { // UTF-16LE // Chinese & Korean
                    if (currentLanguage.equalsIgnoreCase("." + sCountry)) {
                        String contentString = new String(byteText, Def.SERVER_STRING_FORMAT);
                        deString = contentString + "\n\n" + deString;

                        if (isSpeak) {
                            strSpeak = contentString + "\n\n" + strSpeak;
                        }
                    } else {
                        deString += new String(byteText, Def.SERVER_STRING_FORMAT) + "\n\n";
                    }
                    content = new String(byteText, Def.SERVER_STRING_FORMAT);
                } else if (nType == 0) { //Navi code & Facility code
                    boolean isContainUFT16 = false;
                    UniversalDetector detector = new UniversalDetector();
                    detector.handleData(byteText);
                    detector.dataEnd();
                    if (detector.getDetectedCharset() != null && detector.getDetectedCharset().equals("SHIFT_JIS")) {
                        deString += new String(byteText, Def.STRING_FORMAT_JAPANESE);
                        if (deString.contains("�")) {
                            deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                        }
                    } else if (detector.getDetectedCharset() != null && detector.getDetectedCharset().equals("WINDOWS-1252")) {
                        deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                    } else {
                        //Detect UTF16
                        for (int it = 0; it < byteText.length - 1; it++) {
                            if (it + 1 > byteText.length) break;
                            if (byteText[it] == 0 && byteText[it + 1] != 0) {
                                isContainUFT16 = true;
                                break;
                            }
                        }

                        if (isContainUFT16) {
                            deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                        } else {
                            deString = new String(contentByte, Def.SERVER_STRING_FORMAT_UTF8);
                        }
                    }
//                    checkNaviORFacility(deString);
                    if (isNavi && isFacility) {
//                        gotoCodeList();
                        return;
                    } else {
                        if (directions.size() == 1) {
                            if (codeID != null) {
                                directions.get(0).setCodeID(codeID);
                            }
                            if (companyID != null) {
                                directions.get(0).setCompanyID(Integer.parseInt(companyID));
                            }
//                            Utils.logFirebaseAnalytics(context, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, directions.get(0).getCoursename(), codeID,
//                                    String.valueOf(companyID), projectID);
//                            gotoLeading();
                            return;
                        } else if (directions.size() > 1) {
                            for (Direction direction : directions) {
                                if (codeID != null) {
                                    direction.setCodeID(codeID);
                                }
                                if (companyID != null) {
                                    direction.setCompanyID(Integer.parseInt(companyID));
                                }
//                                Utils.logFirebaseAnalytics(context, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, direction.getCoursename(), codeID, String.valueOf(companyID), projectID);

                            }
//                            gotoCourseList();
                            return;
                        }
                        if (facility != null && isFacility) {
                            if (codeID != null) {
                                facility.setCodeID(codeID);
                            }
                            if (companyID != null) {
                                facility.setCompanyID(Integer.parseInt(companyID));
                            }
//                            Utils.logFirebaseAnalytics(context, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, facility.getBrochureName(), codeID,
//                                    String.valueOf(companyID), projectID);
//                            gotoFacilityMap();
                            return;
                        }
                    }
                } else if (nType == 10) { // code ID
                    strCodeID = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                    Log.i("grape ", "strCodeID " + strCodeID);
                }
                mLanguageMap.put("." + sCountry, content);
                if (isSave) {
                    strSave += content;
                    if (isSpeak) {
                        strSavedSpeak += content;
                    }
                }

                if (!strCodeID.isEmpty()) {
                    extractID(strCodeID);
                }

                if (!deString.equals("")) {
                    content=deString;
                    savedContent = strSave;
                    speakContent=strSpeak;
                    savedSpeakContent=strSavedSpeak;
                } else {
                    content = deString;
                    savedContent=" ";
                    speakContent=" ";
                    savedSpeakContent=" ";
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!hasGotoResult) {
                //TODO
//                gotoResultScreen();
            }
        }
    }

    protected void detectNoTagCode() {
        //TODO: Show noTag Code
    }

    private boolean isDigitOnly(String str) {
        if (str.equals("")) {
            return false;
        } else {
            return TextUtils.isDigitsOnly(str);
        }
    }

    public void setBrightness(int min, int max) {    // grape
        brightnessMin = min;
        brightnessMax=max;
    }

    public String decodeImage() {
        setUvDecodeSymMode(0);
        AssetManager manager = context.getAssets();
        int javisReuslt = callJavis(width, height, data, 0, manager);
        Log.d("Decode", "javisReuslt = " + javisReuslt);

        if (javisReuslt > 0) {
            if (invalid == -45) {
//                detectInvalidUniCode();
                return null;
            }
            if (countRun == 1) return null;
            countRun = 1;
            // Old JAVIS
            if (bufferType == 3) {
                try {
                    String deString = new String(contentByte, Def.STRING_FORMAT_JAPANESE);
                    if (deString.contains("�")) {
                        deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                    }
                    if (deString.contains("<tag")) {
                        savedContent = deString;
                        speakContent = deString;
                        savedSpeakContent= deString;
                        content = deString;
                    } else {
                        if (!isDigitOnly(deString)) {
                            if (JapanesePattern.isJapanese(deString)) {
                                savedContent = deString;
                                speakContent = deString;
                               savedSpeakContent= deString;
                                content = deString;
                                isJapanese = true;
                            } else {
                                detectNoTagCode();
                                countRun = 0;
                                return null;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (bufferType == 4) { // New JAVIS
                int i = 0;
                String deString = "";
                String strSave = "";
                String strSpeak = "";
                String strSavedSpeak = "";
                String strCodeID = "";      // code ID

                while (i < contentByte.length - 1) {
                    int nType = 0;
                    String sCountry = "";
                    String content = "";
                    int nAttr = 0;
                    int nLength = 0;

                    boolean isSpeak = true;
                    boolean isSave = true;

                    // Attribute
                    nAttr = (int) (contentByte[i] & UVTB_TYPE_ATTR_MASK);
                    // DataType
//                    nType = (int) contentByte[i++];
                    nType = (int) (contentByte[i++] & UVTB_TYPE_MASK);
                    Log.i("grape", "type = " + nType);
                    if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                            == UVTB_TYPE_ATTR_NO_SPEAK) {
                        Log.i("grape", "attr = no_speak");
                        isSpeak = false;
                    } else if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                            == UVTB_TYPE_ATTR_NO_SAVE) {
                        Log.i("grape", "attr = no_save");
                        isSave = false;
                    } else if ((nAttr & (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE))
                            == (UVTB_TYPE_ATTR_NO_SPEAK | UVTB_TYPE_ATTR_NO_SAVE)) {
                        Log.i("grape", "attr = no_speak, no_save");
                        isSpeak = false;
                        isSave = false;
                    }
                    // CountryCode
                    sCountry = new String(new byte[]{contentByte[i++]});
                    sCountry += new String(new byte[]{contentByte[i++]});
                    sCountry += new String(new byte[]{contentByte[i++]});

                    // Length
                    int sByte1 = contentByte[i++];
                    int sByte2 = contentByte[i++];
                    if (sByte1 < 0) {
                        sByte1 += 256;
                    }
                    if (sByte2 < 0) {
                        sByte2 += 256;
                    }
                    nLength = sByte1;
                    nLength += sByte2 * 256;

                    // TextData
                    byte[] byteText = new byte[nLength];
                    for (int j = 0; j < nLength; j++) {
                        byteText[j] = contentByte[i];
                        i++;
                    }

                    // Convert
                    try {
                        // Japanese
                        if (nType == 1 || nType == 4) { // SJIS / Ascii(ISO/IEC-8859-1
                            if (LanguageSupport.checkLanguages("." + sCountry) == 0) {
                                String contentString = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                                if(currentLanguage.equals("." + sCountry)){
                                    deString = contentString + "\n\n" + deString;
                                }else{
                                    deString = deString + "\n\n" + contentString;
                                }

                                Log.d("Scan Activity ", "deString " + deString);
                                if (isSpeak) {
                                    strSpeak = contentString + "\n\n" + strSpeak;
                                    Log.d("Scan Activity ", "strSpeak " + strSpeak);
                                }
                            } else {
                                deString += new String(byteText, Def.STRING_FORMAT_JAPANESE) + "\n\n";
                            }
                            content = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                        } else if (nType == 2) { // UTF-8 // English
                            if (LanguageSupport.checkLanguages("." + sCountry) == 0) {
                                String contentString = new String(byteText, Def.SERVER_STRING_FORMAT);
                                if(currentLanguage.equals("." + sCountry)){
                                    deString = contentString + "\n\n" + deString;
                                }else{
                                    deString = deString + "\n\n" + contentString;
                                }
                                if (isSpeak) {
                                    strSpeak = strSpeak + "\n\n" + contentString;
                                }
                            } else {
                                deString += new String(byteText, Def.SERVER_STRING_FORMAT) + "\n\n";
                            }
                            content = new String(byteText, Def.SERVER_STRING_FORMAT);
                        } else if (nType == 3) { // UTF-16LE // Chinese & Korean
                            if (LanguageSupport.checkLanguages("." + sCountry) == 0) {
                                String contentString = new String(byteText, Def.SERVER_STRING_FORMAT);
                                if(currentLanguage.equals("." + sCountry)){
                                    deString = contentString + "\n\n" + deString;
                                }else{
                                    deString = deString + "\n\n" + contentString;
                                }

                                if (isSpeak) {
                                    strSpeak = strSpeak + "\n\n" + contentString;
                                }
                            } else {
                                deString += new String(byteText, Def.SERVER_STRING_FORMAT) + "\n\n";
                            }
                            content = new String(byteText, Def.SERVER_STRING_FORMAT);
                        } else if (nType == 0) { //Navi code & Facility code
                            boolean isContainUFT16 = false;
                            UniversalDetector detector = new UniversalDetector();
                            detector.handleData(byteText);
                            detector.dataEnd();

                            if (detector.getDetectedCharset() != null && detector.getDetectedCharset().equals("SHIFT_JIS")) {
                                deString += new String(byteText, Def.STRING_FORMAT_JAPANESE);
                                if (deString.contains("�")) {
                                    deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                                }
                            } else if (detector.getDetectedCharset() != null && detector.getDetectedCharset().equals("WINDOWS-1252")) {
                                deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                            } else {
                                //Detect UTF16
                                for (int it = 0; it < byteText.length - 1; it++) {
                                    if (it + 1 > byteText.length) break;
                                    if (byteText[it] == 0 && byteText[it + 1] != 0) {
                                        isContainUFT16 = true;
                                        break;
                                    }
                                }

                                if (isContainUFT16) {
                                    deString = new String(contentByte, Def.SERVER_STRING_FORMAT);
                                } else {
                                    deString = new String(contentByte, Def.SERVER_STRING_FORMAT_UTF8);
                                }
                            }
//                            checkNaviORFacility(deString);
                            if (isNavi && isFacility) {
//                                gotoCodeList();
                                return null;
                            } else {
                                if (directions.size() == 1) {
                                    if (codeID != null) {
                                        directions.get(0).setCodeID(codeID);
                                    }
                                    if (companyID != null) {
                                        directions.get(0).setCompanyID(Integer.parseInt(companyID));
                                    }
//                                    Utils.logFirebaseAnalytics(this, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, directions.get(0).getCoursename(), codeID,
//                                            String.valueOf(companyID), projectID);
//                                    gotoLeading();
                                    return null;
                                } else if (directions.size() > 1) {
                                    for (Direction direction : directions) {
                                        if (codeID != null) {
                                            direction.setCodeID(codeID);
                                        }
                                        if (companyID != null) {
                                            direction.setCompanyID(Integer.parseInt(companyID));
                                        }
//                                        Utils.logFirebaseAnalytics(this, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, direction.getCoursename(), codeID, String.valueOf(companyID), projectID);
                                    }
//                                    gotoCourseList();
                                    return null;
                                }
                                if (facility != null && isFacility) {
                                    if (codeID != null) {
                                        facility.setCodeID(codeID);
                                    }
                                    if (companyID != null) {
                                        facility.setCompanyID(Integer.parseInt(companyID));
                                    }
//                                    Utils.logFirebaseAnalytics(this, Def.EVENT_SCAN, Def.CODE_NAME_ITEM, facility.getBrochureName(), codeID,
//                                            String.valueOf(companyID), projectID);
//                                    gotoFacilityMap();
                                    return null;
                                }
                            }
                        } else if (nType == 10) { // code ID
                            strCodeID = new String(byteText, Def.STRING_FORMAT_JAPANESE);
                            Log.i("grape ", "strCodeID " + strCodeID);
                        }
                        if (!sCountry.equals("%ci")) {
                            mLanguageMap.put("." + sCountry, content);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (!strCodeID.isEmpty()) {
                    extractID(strCodeID);
                }

                if (!deString.equals("")) {
                    content = deString;
                    savedContent = strSave;
                    speakContent = strSpeak;
                    savedSpeakContent = strSavedSpeak;
                } else {
                    content = deString;
                    savedContent = " ";
                    speakContent = " ";
                    savedSpeakContent = " ";
                }
                
                //TODO: here
                Log.d("decode", deString);
                return  deString;
            }

//            gotoResultScreen();
            // If result is -17 or -18, that means the rectangle is similar as voice code
        } else if (javisReuslt == -17 || javisReuslt == -18) {
//            mIconScanning.setVisibility(View.VISIBLE);
//            mIconScanning.setBackgroundResource(R.drawable.anim_scanning);
//            AnimationDrawable anim = (AnimationDrawable) mIconScanning.getBackground();
//            anim.start();
        }
        return null;
    }

    private void checkNaviORFacility(String input) {
        Pattern p = Pattern.compile("<tag\\s*(lang=[\"|”][^<]*[\"|”]>([^<]*))</tag>");
        Matcher m = p.matcher(input);
        while (m.find()) {
            String key = m.group(1).replaceFirst(" ", "").substring(6, 9);
//            String codeID = m.group(2).trim().split("\r\n")[0];
            if (key.equals(Def.NAVI_CODE)) {
                executeNaviCode(m.group(2));
                isNavi = true;

            }
            if (key.equals(Def.FACILITY_CODE)) {
                executeFacilityCode(m.group(2));
                isFacility = true;
            }
            if (key.equals(Def.CODE_ID)) {
                extractID(m.group(2));
            }
        }
    }

    private void executeNaviCode(String value) {
        String[] values = value.trim().split("\r\n");
        Direction direction = new Direction();
        ArrayList<Information> infoList = new ArrayList<>();
        direction.setCodeType(values[0]);
        String[] languageCode = values[1].split("\\|");
        String[] coursename = values[2].split("\\|");
        if (coursename == null) {
            direction.setCoursename("");
        } else {
            if (coursename.length > 1) {
                int engIndex = -1;
                for (int i = 0; i < languageCode.length; i++) {
                    if (languageCode[i].toLowerCase().equals("eng")) {
                        engIndex = i;
                    }
                    if (deviceLanguage.equals(languageCode[i].toLowerCase())) {
                        direction.setCoursename(coursename[i]);
                        break;
                    } else if (i == languageCode.length - 1) {
                        if (engIndex >= 0) {
                            direction.setCoursename(coursename[engIndex]);
                        } else {
                            direction.setCoursename(coursename[0]);

                        }
                    }
                }
            } else {
                direction.setCoursename(coursename.length == 0 ? "" : coursename[0]);

            }
        }
        for (int j = 3; j < values.length; j++) {
            String[] info = values[j].split(";", -1);
            information = new Information();
            information.setPointnumber(info[0]);
            String[] pointname = info[1].split("\\|", -1);
            String[] barrierInfo = info[5].split("\\|", -1);
            if (pointname == null) {
                information.setPointname("");
            } else {
                if (pointname.length > 1) {
                    int engIndex = -1;
                    for (int i = 0; i < languageCode.length; i++) {
                        if (languageCode[i].toLowerCase().equals("eng")) {
                            engIndex = i;
                        }
                        if (deviceLanguage.equals(languageCode[i].toLowerCase())) {
                            information.setPointname(pointname[i]);
                            if (i < barrierInfo.length) {
                                information.setBarrier_info(barrierInfo[i]);
                            }
                            break;
                        } else if (i == languageCode.length - 1) {
                            if (engIndex >= 0) {
                                information.setPointname(pointname[engIndex]);
                                if (engIndex < barrierInfo.length) {
                                    information.setBarrier_info(barrierInfo[engIndex]);

                                }
                            } else {
                                information.setPointname(pointname[0]);
                                information.setBarrier_info(barrierInfo[0]);
                            }

                        }
                    }
                } else {
                    information.setPointname(pointname.length == 0 ? "" : pointname[0]);
                    information.setBarrier_info(barrierInfo.length == 0 ? null : barrierInfo[0]);
                }
            }

//            information.setPointname(info[1]);
            StringBuilder builder = new StringBuilder();
            builder.append(info[2] + ",");
            builder.append(info[3]);
            information.setPosition_info(builder.toString());
            information.setBeacon_id(info[4]);

            infoList.add(information);
        }
        direction.setInfos(infoList);
        directions.add(direction);
    }

    private void executeFacilityCode(String value) {
        int duplicate = 0;
        String[] values = value.trim().split("\r\n");
        ArrayList<Information> infoList = new ArrayList<>();
        facility = new Facility();
        facility.setCodeTypes(values[0]);
        String[] languageCode = values[1].split("\\|");
        String[] brochurename = values[2].split("\\|");
        if (brochurename == null) {
            facility.setBrochureName("");
        } else {
            if (brochurename.length > 1) {
                int engIndex = -1;
                for (int i = 0; i < languageCode.length; i++) {
                    if (languageCode[i].toLowerCase().equals("eng")) {
                        engIndex = i;
                    }
                    if (deviceLanguage.equals(languageCode[i].toLowerCase())) {
                        facility.setBrochureName(brochurename[i]);
                        break;
                    } else if (i == languageCode.length - 1) {
                        if (engIndex >= 0) {
                            facility.setBrochureName(brochurename[engIndex]);

                        } else {
                            facility.setBrochureName(brochurename[0]);
                        }
                    }
                }
            } else {
                facility.setBrochureName(brochurename.length == 0 ? "" : brochurename[0]);
            }
        }
        for (int j = 3; j < values.length; j++) {
            information = new Information();
            String[] info = values[j].trim().split(";");
            if (info.length > 4) {
                String[] infoType = info[4].split("\\|");
                if (infoType.length > 1) {
                    int engIndex = -1;
                    for (int i = 0; i < languageCode.length; i++) {
                        if (languageCode[i].toLowerCase().equals("eng")) {
                            engIndex = i;
                        }
                        if (deviceLanguage.equals(languageCode[i].toLowerCase())) {
                            information.setBarrier_info(infoType[i]);
                            break;
                        } else if (i == languageCode.length - 1) {
                            if (engIndex >= 0) {
                                information.setBarrier_info(infoType[engIndex]);

                            } else {
                                information.setBarrier_info(infoType[0]);
                            }
                        }
                    }
                } else {
                    information.setBarrier_info(infoType.length == 0 ? null : infoType[0]);
                }
            }
            if (info[1].contains("\n") || info[2].contains("\n")) {
                continue;
            }

            String[] pointname = info[0].split("\\|");
            if (pointname == null) {
                information.setPointname("");
            }
            if (pointname.length > 1) {
                int engIndex = -1;
                for (int i = 0; i < languageCode.length; i++) {
                    if (languageCode[i].toLowerCase().equals("eng")) {
                        engIndex = i;
                    }
                    if (deviceLanguage.equals(languageCode[i].toLowerCase())) {
                        information.setPointname(pointname[i]);
                        break;
                    } else if (i == pointname.length - 1) {
                        if (engIndex >= 0) {
                            information.setPointname(pointname[engIndex]);
                        } else {
                            information.setPointname(pointname[0]);
                        }

                    }
                }
            } else {
                information.setPointname(pointname.length == 0 ? "" : pointname[0]);
            }
            StringBuilder builder = new StringBuilder();
            builder.append(info[1] + ",");
            builder.append(info[2]);
            information.setPosition_info(builder.toString());

            String[] categories = info[3].split("\\|");
            ArrayList<String> listCate = new ArrayList<>();
            if (categories == null) {
                listCate.add("0");
            } else {
                if (categories.length > 0) {
                    for (int i = 0; i < categories.length; i++) {
                        listCate.add(categories[i]);
                    }
                } else {
                    listCate.add("0");
                }
            }
            information.setFacilityCategory(listCate);
            infoList.add(information);
        }
        facility.setInfos(infoList);

        if (codeID != null) {
            facility.setCodeID(codeID);
        }
        if (companyID != null) {
            facility.setCompanyID(Integer.parseInt(companyID));
        }
        facilities.add(facility);
    }

    private void extractID(String strCodeID) {
        String[] ids = strCodeID.trim().split("\r\n");
        if (ids.length < 3) return;
        codeID = ids[0];
        companyID = ids[1];
        projectID = ids[2];
    }
}

