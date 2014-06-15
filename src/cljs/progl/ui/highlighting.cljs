(ns progl.ui.highlighting
  (:require [cljs.core.async :refer [chan mult]]))

(def highlight-in (chan))
(def highlight-out (mult highlight-in))

(def dehighlight-in (chan))
(def dehighlight-out (mult dehighlight-in))
