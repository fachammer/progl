(ns progl.ui.list
  (:require-macros [c2.util :refer [bind!]])
  (:require [c2.event :refer [on]]
            [c2.core :refer [unify]]
            [c2.dom :refer [attr]]
            [progl.ui.core :as ui]
            [progl.dom :as dom]
            [progl.ui.select :as select]
            [progl.util :as util :refer [on-channel]]
            [cljs.core.async :as async :refer [put!]]))

(defn creators-label [creators]
  (str "Creator" (when-not (= 1 (count creators)) "s") ": "))

(defn creators-entry [creators]
  (when-not (empty? creators) [:tr [:td (creators-label creators)] [:td (apply str (interpose ", " creators))]]))

(defn table-entry
  ([label value]
   (table-entry label value false))
  ([label value hidden?]
    [:tr {:class (if hidden? "hidden" "")} [:td label] [:td value]]))

(defn table-entry-when-not-empty [label value]
  (if (empty? value)
    (table-entry label value true)
    (table-entry label value false)))

(defn year-entry [year]
  (table-entry "Appearance year: " year))

(defn lang-link [lang-key lang-name]
  [:a {:class (name lang-key) :langk (name lang-key) :href "#"} lang-name])

(defn influences-entry [influences languages]
  (table-entry-when-not-empty "Influenced by: " (interpose ", " (map #(->> (% languages) :name (lang-link %)) influences))))

(defn influenced-entry [influenced languages]
  (table-entry-when-not-empty "Influenced: " (interpose ", " (map #(->> (% languages) :name (lang-link %)) influenced))))

(defn lang-list-item [[[langk {lang-name :name
                              creators :creators
                              year :appearance-year
                              influences :influenced-by
                              influenced :influenced}] languages]]
  [:li {:class (str "active " (name langk))}
   [:div lang-name
    [:table {:class (name langk)}
     (year-entry year)
     (creators-entry creators)
     (influences-entry influences languages)
     (influenced-entry influenced languages)]]])

(defn link? [node]
  (= "A" (-> node .-tagName)))

(defn with-quotes [s]
  (str "\"" s "\""))

(defn on-lang-list-item-click [[[_ {lang-name :name}] _] _ evt]
  (let [target (.-target evt)]
    (if (link? target)
      (put! select/in (with-quotes (.-innerHTML target)))
      (put! select/in (with-quotes lang-name)))))

(defn on-lang-list-item-mouseover [[[langk _] _] _ evt]
  (let [event-target (-> evt .-target)]
    (when (link? event-target)
      (ui/highlight-lang! (attr event-target :langk)))
    (ui/highlight-lang! langk)))

(defn on-lang-list-item-mouseout [_ _ _]
  (ui/remove-highlights!))

(def list-langs (atom nil))

(defn select-langs [langs]
  (swap! list-langs (fn [_] (sort-by #(-> % val :name) langs))))

(defn language-list [id langs]
  (select-langs langs)
  (on-channel select/out select-langs)
  (let [css-id (str "#" (name id))]
    (bind! css-id (unify (map #(vector % langs) @list-langs) lang-list-item))
    (on css-id :click on-lang-list-item-click)
    (on css-id :mouseover on-lang-list-item-mouseover)
    (on css-id :mouseout on-lang-list-item-mouseout)))
