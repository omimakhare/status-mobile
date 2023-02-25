(ns quo2.components.counter.counter.view
  (:require
    [quo2.components.markdown.text :as text]
    [quo2.foundations.colors :as colors]
    [react-native.core :as rn]
    [utils.number]
    [quo2.theme :as quo.theme]
    utils.schema
    malli.util
    [quo2.components.counter.counter.style :as style]))

(defn- view-internal
  [{:keys [type customization-color theme container-style accessibility-label max-value]
    :or   {max-value 99 customization-color :blue theme :dark}}
   value]
  (let [type  (or type :default)
        value (utils.number/parse-int value)
        label (if (> value max-value)
                (str max-value "+")
                (str value))]
    [rn/view
     {:test-ID             :counter-component
      :accessible          true
      :accessibility-label accessibility-label
      :style               (style/container
                            {:label               label
                             :type                type
                             :customization-color customization-color
                             :theme               theme
                             :container-style     container-style
                             :value               value
                             :max-value           max-value})}
     [text/text
      {:weight :medium
       :size   :label
       :style  (when (= type :default) {:color colors/white})}
      label]]))

(def view
  (utils.schema/instrument
   ::counter
   [:=>
    [:cat
     (malli.util/optional-keys
      [:map {:closed true}
       [:accessibility-label :keyword]
       [:container-style :schema.common/style]
       [:customization-color :schema.common/color]
       [:max-value :int]
       [:theme :schema.common/theme]
       [:type [:enum :default :secondary :grey :outline]]])
     [:maybe [:or :int :string]]]
    :any]
   (quo.theme/with-theme view-internal)))
