package com.xeyqe.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class myMemory extends AppCompatActivity {

    private EditText editText;
    private TextView textView;
    private RequestQueue mQueue;
    private String inputLanguage = "en-GB";
    private String outputLanguage = "cs-CZ";

    private HashMap<String, String> map = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_memory);

        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textview);
        mQueue = Volley.newRequestQueue(this);
        Button buTranslate = findViewById(R.id.buTranslate);

        map.put("Afrikaans", "af-ZA");
        map.put("Albanian", "sq-AL");
        map.put("Amharic", "am-ET");
        map.put("Arabic", "ar-SA");
        map.put("Armenian", "hy-AM");
        map.put("Azerbaijani", "az-AZ");
        map.put("Bajan", "bjs-BB");
        map.put("Balkan Gipsy", "rm-RO");
        map.put("Basque", "eu-ES");
        map.put("Bemba", "bem-ZM");
        map.put("Bengali", "bn-IN");
        map.put("Bielarus", "be-BY");
        map.put("Bislama", "bis-VU");
        map.put("Bosnian", "bs-BA");
        map.put("Breton", "br-FR");
        map.put("Bulgarian", "bg-BG");
        map.put("Burmese", "my-MM");
        map.put("Catalan", "ca-ES");
        map.put("Cebuano", "cb-PH");
        map.put("Chamorro", "cha-GU");
        map.put("Chinese (Simplified)", "zh-CN");
        map.put("Chinese Traditional", "zh-TW");
        map.put("Comorian (Ngazidja)", "zdj-KM");
        map.put("Coptic", "cop-EG");
        map.put("Creole English (Antigua and Barbuda)", "aig-AG");
        map.put("Creole English (Bahamas)", "bah-BS");
        map.put("Creole English (Grenadian)", "gcl-GD");
        map.put("Creole English (Guyanese)", "gyn-GY");
        map.put("Creole English (Jamaican)", "jam-JM");
        map.put("Creole English (Vincentian)", "svc-VC");
        map.put("Creole English (Virgin Islands)", "vic-US");
        map.put("Creole French (Haitian)", "ht-HT");
        map.put("Creole French (Saint Lucian)", "acf-LC");
        map.put("Creole French (Seselwa)", "crs-SC");
        map.put("Creole Portuguese (Upper Guinea)", "pov-GW");
        map.put("Croatian", "hr-HR");
        map.put("Czech", "cs-CZ");
        map.put("Danish", "da-DK");
        map.put("Dutch", "nl-NL");
        map.put("Dzongkha", "dzo-BT");
        map.put("English", "en-GB");
        map.put("Esperanto", "eo-EU");
        map.put("Estonian", "et-EE");
        map.put("Fanagalo", "fn-FNG");
        map.put("Faroese", "fo-FO");
        map.put("Finnish", "fi-FI");
        map.put("French", "fr-FR");
        map.put("Galician", "gl-ES");
        map.put("Georgian", "ka-GE");
        map.put("German", "de-DE");
        map.put("Greek", "el-GR");
        map.put("Greek (Classical)", "grc-GR");
        map.put("Gujarati", "gu-IN");
        map.put("Hausa", "ha-NE");
        map.put("Hawaiian", "haw-US");
        map.put("Hebrew", "he-IL");
        map.put("Hindi", "hi-IN");
        map.put("Hungarian", "hu-HU");
        map.put("Icelandic", "is-IS");
        map.put("Indonesian", "id-ID");
        map.put("Inuktitut (Greenlandic)", "kal-GL");
        map.put("Irish Gaelic", "ga-IE");
        map.put("Italian", "it-IT");
        map.put("Japanese", "ja-JA");
        map.put("Javanese", "jw-ID");
        map.put("Kabuverdianu", "kea-CV");
        map.put("Kabylian", "kab-DZ");
        map.put("Kannada", "ka-IN");
        map.put("Kazakh", "kk-KZ");
        map.put("Khmer", "km-KM");
        map.put("Kinyarwanda", "rw-RW");
        map.put("Kirundi", "run-RN");
        map.put("Korean", "ko-KR");
        map.put("Kurdish", "ku-TR");
        map.put("Kyrgyz", "ky-KG");
        map.put("Lao", "lo-LA");
        map.put("Latin", "la-VA");
        map.put("Latvian", "lv-LV");
        map.put("Lithuanian", "lt-LT");
        map.put("Luxembourgish", "lb-LU");
        map.put("Macedonian", "mk-MK");
        map.put("Malagasy", "mg-MG");
        map.put("Malay", "ms-MY");
        map.put("Maldivian", "div-MV");
        map.put("Maltese", "mt-MT");
        map.put("Manx Gaelic", "gv-IM");
        map.put("Maori", "mi-NZ");
        map.put("Marshallese", "mh-MH");
        map.put("Mende", "men-SL");
        map.put("Mongolian", "mn-MN");
        map.put("Morisyen", "mfe-MU");
        map.put("Nepali", "ne-NP");
        map.put("Niuean", "niu-NU");
        map.put("Norwegian", "no-NO");
        map.put("Nyanja", "ny-MW");
        map.put("Pakistani", "ur-PK");
        map.put("Palauan", "pau-PW");
        map.put("Panjabi", "pa-IN");
        map.put("Papiamentu", "pap-PAP");
        map.put("Pashto", "ps-PK");
        map.put("Persian", "fa-IR");
        map.put("Pijin", "pis-SB");
        map.put("Polish", "pl-PL");
        map.put("Portuguese", "pt-PT");
        map.put("Potawatomi", "pot-US");
        map.put("Quechua", "qu-PE");
        map.put("Romanian", "ro-RO");
        map.put("Russian", "ru-RU");
        map.put("Samoan", "smo-WS");
        map.put("Sango", "sg-CF");
        map.put("Scots Gaelic", "gd-GB");
        map.put("Serbian", "sr-RS");
        map.put("Shona", "sna-ZW");
        map.put("Sinhala", "si-LK");
        map.put("Slovak", "sk-SK");
        map.put("Slovenian", "sl-SI");
        map.put("Somali", "so-SO");
        map.put("Sotho, Southern", "nso-ZA");
        map.put("Spanish", "es-ES");
        map.put("Sranan Tongo", "srn-SR");
        map.put("Swahili", "sw-SZ");
        map.put("Swedish", "sv-SE");
        map.put("Swiss German", "de-CH");
        map.put("Syriac (Aramaic)", "syc-TR");
        map.put("Tagalog", "tl-PH");
        map.put("Tajik", "tg-TJ");
        map.put("Tamashek (Tuareg)", "tmh-DZ");
        map.put("Tamil", "ta-LK");
        map.put("Telugu", "te-IN");
        map.put("Tetum", "tet-TL");
        map.put("Thai", "th-TH");
        map.put("Tibetan", "bod-CN");
        map.put("Tigrinya", "ti-TI");
        map.put("Tok Pisin", "tpi-PG");
        map.put("Tokelauan", "tkl-TK");
        map.put("Tongan", "ton-TO");
        map.put("Tswana", "tsn-BW");
        map.put("Turkish", "tr-TR");
        map.put("Turkmen", "tk-TM");
        map.put("Tuvaluan", "tvl-TV");
        map.put("Ukrainian", "uk-UA");
        map.put("Uma", "ppk-ID");
        map.put("Uzbek", "uz-UZ");
        map.put("Vietnamese", "vi-VN");
        map.put("Wallisian", "wls-WF");
        map.put("Welsh", "cy-GB");
        map.put("Wolof", "wo-SN");
        map.put("Xhosa", "xh-ZA");
        map.put("Yiddish", "yi-YD");
        map.put("Zulu", "zu-ZA");


        buTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jsonParse();
            }
        });

        final Button buInputLanguage = findViewById(R.id.buInputLanguage);
        final Button buOutputLanguage = findViewById(R.id.buOutputLanguage);

        buInputLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu1 = new PopupMenu(myMemory.this, buInputLanguage);
                popupMenu1.getMenuInflater().inflate(R.menu.popup_menu, popupMenu1.getMenu());

                popupMenu1.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        inputLanguage = map.get(item.getTitle().toString());
                        buInputLanguage.setText(item.getTitle().toString());
                        return true;
                    }
                });

                popupMenu1.show();
            }
        });

        buOutputLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu2 = new PopupMenu(myMemory.this, buOutputLanguage);
                popupMenu2.getMenuInflater().inflate(R.menu.popup_menu, popupMenu2.getMenu());

                popupMenu2.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        outputLanguage = map.get(item.getTitle().toString());
                        buOutputLanguage.setText(item.getTitle().toString());
                        return true;
                    }
                });

                popupMenu2.show();
            }
        });


    }

    private void jsonParse() {

        String userInput = editText.getText().toString();

        String url = "https://api.mymemory.translated.net/get?q="+userInput+"&langpair="+inputLanguage+"|"+outputLanguage;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject responseData = response.getJSONObject("responseData");
                            String translatedText = responseData.getString("translatedText");
                            textView.setText(translatedText);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
            }
        });

        mQueue.add(request);
    }
}
