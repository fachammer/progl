(ns progl.language-scraper
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as s]))

(def url-prefix "http://en.wikipedia.org")
(def tl-article "/wiki/Timeline_of_programming_languages")

(defn wiki-url [path]
  (str url-prefix path))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn fetch-wiki-article [path]
  (fetch-url (wiki-url path)))

(def raw-tl (fetch-wiki-article tl-article))

(defn select-wikitable [markup]
  (html/select markup [:table.wikitable]))

(defn select-rows [markup]
  (html/select markup [:tr]))

(defn td-childs [rows n]
  (html/select rows [[:td (html/nth-child n)]]))

(def col-key->child-number {:years 1
                            :names 2
                            :creators 3
                            :predecessors 4})

(defn select [col-key rows]
  (td-childs rows (col-key col-key->child-number)))

(defn select-texts [col-key rows]
  (html/texts (select col-key rows)))

(defn select-years [rows]
  (select-texts :years rows))

(defn select-names [rows]
  (select-texts :names rows))

(defn select-creators [rows]
  (select-texts :creators rows))

(defn select-predecessors [rows]
  (select-texts :predecessors rows))

(defn select-url [{:keys [content]}]
  (html/select content [[:a (html/but (html/attr-ends :title " (page does not exist)"))]]))

(defn get-href [content]
  (when-let [url (select-url content)]
    (-> url first :attrs :href)))

(defn select-urls [rows]
  (map get-href (select :names rows)))

(defn parse-years [years]
  (map #(read-string (subs % 0 4)) years))

(defn parse-name [lang-name]
  (if (= lang-name "SQL aka structured query language")
    "SQL"
    (s/replace lang-name #" \(.*\).*$" "")))

(defn parse-names [names]
  (map parse-name names))

(defn parse-creator [creator]
  (apply hash-set (remove #(= % "") (map s/trim (s/split creator #",")))))

(defn parse-creators [creators]
  (map parse-creator creators))

(def name->keyword-exceptions {"GOM – Good Old Mad" :gom
                               "MAD – Michigan Algorithm Decoder" :mad
                               "Korn Shell" :ksh
                               "Bourne Shell" :sh
                               "Z Shell" :zsh
                               "JOSS" :joss-i
                               "Business BASIC" :basic
                               "ARC" :arc-assembly
                               "ALGOL" :algol-58
                               "Delphi" :object-pascal
                               "dBase" :vulcan-dbase-ii
                               "Licenced from Microsoft" :basic
                               "SORT/MERGE" :sort-merge-generator
                               "Ada" :ada-80
                               "QBasic" :basic
                               "Laning and Zierler" :laning-and-zierler-system
                               "IPL" :ipl-i
                               "Glennie Autocode" :autocode
                               "MetaHaskell" :haskell
                               "A-2" :a-0
                               "Unix shell" :sh
                               "BASIC/Z" :basic
                               "Smalltalk-72" :smalltalk
                               "Algol60" :algol-60
                               "ALGOL?" :algol-58
                               "Java?" :java
                               "Ruby?" :ruby
                               "Standard C" :c
                               "Smalltalk-80" :smalltalk
                               "Boehm" :boehm-unnamed-coding-system
                               "Turbo Pascal" :pascal})



(defn name->keyword [string]
  (keyword
   (if (contains? name->keyword-exceptions string)
     (name->keyword-exceptions string)
     (-> string
         (s/replace #"[()]" "")
         (s/replace #"#" "sharp")
         (s/replace #" |/|:" "-")
         (s/replace #"\+" "PLUS")
         (s/replace #"\." "dot")
         (s/lower-case)))))

(defn language-keywords [names]
  (apply hash-set (map name->keyword names)))

(def language-keywords (apply hash-set (->> raw-tl select-rows select-names parse-names (map name->keyword))))

(defn language-exists? [language-key]
  (contains? language-keywords language-key))

(defn parse-predecessors [predecessors]
  (map #(cond (or (= % "*") (= % "")) #{}
              :else (apply hash-set (filter language-exists? (map name->keyword (map s/trim (s/split % #","))))))
       predecessors))

(defn lang-map-entry [year lang-name creators predecessors url]
  {(name->keyword lang-name){:appearance-year year
                        :name lang-name
                        :creators creators
                        :influenced-by predecessors
                        :url url}})

(defn timeline [rows]
  (apply merge (map lang-map-entry
                    (->> rows select-years parse-years)
                    (->> rows select-names parse-names)
                    (->> rows select-creators parse-creators)
                    (->> rows select-predecessors parse-predecessors)
                    (->> rows select-urls))))

(defn progl-timeline []
  (-> (fetch-wiki-article tl-article) select-wikitable select-rows timeline))

(defn influenced-languages [lang-key languages]
  (apply hash-set (keys (filter #(contains? (:influenced-by (val %)) lang-key) languages))))

(defn assoc-influenced [[lang-key lang] languages]
  [lang-key (assoc lang :influenced (influenced-languages lang-key languages))])

(defn with-influenced [languages]
  (apply hash-map (flatten (map #(assoc-influenced % languages) languages))))

(with-influenced (progl-timeline))

(defn select-infobox [markup]
  (html/select markup [:table.infobox]))

(defn select-table-heads [table]
  (html/select table [:th]))

(defn text-equals [text]
  #(= (html/text %) text))

(defn select-table-cell [table cell-title]
  (apply str (html/texts
              (html/select table [[:td (html/left
                                        [[:th (html/pred (text-equals cell-title))]])]]))))

(defn select-paradigms [table]
  (select-table-cell table "Paradigm(s)"))

(defn select-influenced-by [table]
  (select-table-cell table "Influenced by"))

(def paradigm->keyword-exceptions {"Objectoriented" :object-oriented
                                   "Object-oriented-class-based" :object-oriented
                                   "Class-based-object-oriented" :object-oriented
                                   "Class-based" :object-oriented})

(defn paradigm->keyword [paradigm]
  (if (contains? paradigm->keyword-exceptions paradigm)
    (paradigm->keyword-exceptions paradigm)
    (name->keyword paradigm)))

(defn parse-paradigms [paradigms]
  (apply hash-set (map paradigm->keyword (-> paradigms
                                             (s/trim)
                                             (s/replace #"\n|\[.*\]|([mM]ulti[- ]?[pP]aradigm:? ?)" "")
                                             (s/split #"(, )|(\n)| and ")))))

(defn parse-influenced-by [influenced-by]
  (apply hash-set (map name->keyword (-> influenced-by
                                         (s/trim)
                                         (s/replace #"\n|\[.*\]|\)" "")
                                         (s/split #"(, )|(\n)| and | \(")))))

(parse-influenced-by (select-influenced-by (select-infobox (fetch-wiki-article "/wiki/C_(programming_language)"))))

(defn get-language-data [wiki-article]
  (-> (fetch-wiki-article wiki-article) select-infobox select-paradigms parse-paradigms))

(def raw-lang-data (apply merge (pmap (fn [[lang-key {:keys [url]}]] {lang-key (select-infobox (fetch-wiki-article url))}) (progl-timeline))))

raw-lang-data

(map #(-> % select-influenced-by parse-influenced-by) (vals raw-lang-data))


