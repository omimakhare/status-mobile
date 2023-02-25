(ns status-im2.contexts.quo-preview.tags.number-tag
  (:require [quo2.core :as quo]
            [quo2.components.tags.number-tag.view :as number-tag]
            [react-native.core :as rn]
            [reagent.core :as reagent]
            [status-im2.contexts.quo-preview.preview :as preview]))

(defn preview
  []
  (let [state (reagent/atom {:type   :squared
                             :number "148"
                             :size   :size/s-32
                             :blur?  false})]
    (fn []
      [preview/preview-container
       {:state state :descriptor (preview/generate-descriptor number-tag/?schema)}
       [rn/view
        {:padding-vertical 60
         :flex-direction   :row
         :justify-content  :center}
        [quo/number-tag @state]]])))
