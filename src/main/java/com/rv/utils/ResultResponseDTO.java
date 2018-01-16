package com.rv.utils;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ResultResponseDTO {
    private String text_unique;
    private String unique;
    private ResultJson result_json;
    private ArrayList<SpellCheck> spell_check;
    private SeoCheck seo_check;

    private String error_code;
    private String error_desc;
    private String queuetext;
    private String queueproc;


    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @Getter()
    @Setter
    public static class ResultJson {
        private String date_check;
        private String unique;
        private ArrayList<Urls> urls;
        private String clear_text;
        private String mixed_words;

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        @Setter
        @ToString
        public static class Urls {
            private String url;
            private Integer plagiat;
            private String words;
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class SpellCheck {
        private String error_type;
        private ArrayList<String> replacements;
        private String reason;
        private String error_text;
        private String start;
        private String end;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @ToString
    public static class SeoCheck {
        private String count_chars_with_space;
        private String count_chars_without_space;
        private String count_words;
        private String water_percent;
        private String spam_percent;
        private ArrayList<String> mixed_words;
        private List<KeyGroup.Keys> list_keys;
        private List<KeyGroup> list_keys_group;

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        @Setter
        @ToString
        public static class KeyGroup {
            private String key_title;
            private String count;
            private List<Keys> sub_keys;

            @AllArgsConstructor
            @NoArgsConstructor
            @Getter
            @Setter
            @ToString
            public static class Keys {
                private String key_title;
                private String count;
            }
        }

    }
}
