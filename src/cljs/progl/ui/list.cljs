(ns progl.ui.list
  (:require-macros [c2.util :refer [bind!]]
                   [cljs.core.async.macros :as m :refer [go]])
  (:require [c2.event :refer [on]]
            [c2.core :refer [unify]]
            [c2.dom :refer [attr]]
            [progl.dom :as dom]
            [progl.ui.select :as select]
            [progl.ui.highlighting :as h]
            [progl.util :as util :refer [on-channel with-quotes link? pipe-map<]]
            [progl.query :as q]
            [cljs.core.async :as async :refer [>! chan map< tap mult pipe]]
            [clojure.string :as s]))

(defn table-entry
  ([label value]
   (table-entry label value false))
  ([label value hidden?]
    [:tr {:class (if hidden? "hidden" "")} [:td label] [:td value]]))

(defn table-entry-when-not-empty [label value]
  (if (empty? value)
    (table-entry label value true)
    (table-entry label value false)))

(defn creators-label [creators]
  (str "Creator" (when-not (= 1 (count creators)) "s") ": "))

(defn creators-entry [creators]
  (table-entry-when-not-empty (creators-label creators) (apply str (interpose ", " creators))))

(defn year-entry [year]
  (table-entry "Appearance year: " [:a {:class "year" :href "#"} year]))

(defn lang-link [lang-key lang-name]
  [:a {:class (str "link " (name lang-key)) :href "#" :langk (name lang-key)} lang-name])

(defn influences-entry [influences languages]
  (table-entry-when-not-empty [:a {:class "influences" :href "#"} "Influenced by: "] (interpose ", " (map #(->> (% languages) :name (lang-link %)) influences))))

(defn influenced-entry [influenced languages]
  (table-entry-when-not-empty [:a {:class "influenced" :href "#"} "Influenced: "] (interpose ", " (map #(->> (% languages) :name (lang-link %)) influenced))))

(defn url-entry [url]
  (when url (table-entry "" [:a {:class "url" :href (str "http://en.wikipedia.org" url) :target "_blank"} "More info"])))

(defn lang-list-item [[[langk {lang-name :name
                              creators :creators
                              year :appearance-year
                              influences :influenced-by
                              influenced :influenced
                              url :url}] languages]]
  [:li {:class (str "list " (name langk))}
   [:div [:h3 lang-name]
    [:table {:class (name langk)}
     (year-entry year)
     (creators-entry creators)
     (influences-entry influences languages)
     (influenced-entry influenced languages)
     (url-entry url)]]])

(def list-langs (atom nil))

(defn sort-by-name [langs]
  (sort-by #(-> % val :name) langs))

(def sorted-langs (mult (map< sort-by-name (tap select/out (chan)))))

(defn lang-list-entry [lang-key]
  (dom/element-by-class js/document :list lang-key))

(defn lang-links [lang-key]
  (dom/elements-by-class js/document :link lang-key))

(defn apply-to-list-entries [lang-keys f & args]
  (doseq [lk lang-keys]
    (when-let [list-entry (lang-list-entry lk)]
      (apply f list-entry args))
    (doseq [link (lang-links lk)]
      (apply f link args))))

(defn highlight-langs [[prim-lang & sec-langs]]
  (apply-to-list-entries [prim-lang] dom/add-class! :highlight)
  (when sec-langs
    (apply-to-list-entries sec-langs dom/add-class! :sec-highlight)))

(defn dehighlight-langs [lang-keys]
  (apply-to-list-entries lang-keys dom/remove-class! :highlight)
  (apply-to-list-entries lang-keys dom/remove-class! :sec-highlight))

(defn select-langs [langs]
  (swap! list-langs (fn [_] langs)))

(defn node-click->query [[[[_ {lang-name :name
                               influences :influenced-by
                               influenced :influenced
                               year :appearance-year}] langs] _ evt]]
  (let [target (.-target evt)]
    (if (link? target)
      (case (attr target :class)
        "influences" (s/join "," (map #(-> % langs :name with-quotes) influences))
        "influenced" (s/join "," (map #(-> % langs :name with-quotes) influenced))
        "year" (with-quotes year)
        "url" (.-value (dom/element-by-id :search))
        (with-quotes (.-innerHTML target)))
      (with-quotes lang-name))))

(defn mouseover->lang-key [[[[langk {:keys [influenced influenced-by appearance-year]}] langs] _ evt]]
  (let [target (.-target evt)]
    (if (link? target)
      (concat [langk]
              (case (attr target :class)
                "influences" (vec influenced-by)
                "influenced" (vec influenced)
                "year" (vec (keys (q/query langs (with-quotes appearance-year))))
                "url" []
                [(keyword (attr target :langk))]))
      [langk])))

(defn language-list [id langs]
  ; (select-langs (sort-by-name langs))
  (on-channel sorted-langs select-langs)
  (on-channel h/highlight-out highlight-langs)
  (on-channel h/dehighlight-out dehighlight-langs)
  (let [css-id (str "#" (name id))]
    (bind! css-id (unify (map #(vector % langs) @list-langs) lang-list-item))
    (let [clicks (dom/c2-listen css-id :click)
          mouseovers (dom/c2-listen css-id :mouseover)
          mouseouts (dom/c2-listen css-id :mouseout)]
      (pipe-map< clicks node-click->query select/in)
      (pipe-map< mouseovers mouseover->lang-key h/highlight-in)
      (pipe-map< mouseouts mouseover->lang-key h/dehighlight-in))))
