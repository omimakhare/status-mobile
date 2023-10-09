(ns status-im2.contexts.quo-preview.tags.token-tag
  (:require [quo2.core :as quo]
            [quo2.foundations.resources :as resources]
            [react-native.core :as rn]
            [reagent.core :as reagent]
            [status-im2.contexts.quo-preview.preview :as preview]))

(def descriptor
  [{:key     :size
    :type    :select
    :options [{:key   :big
               :value "big"}
              {:key   :small
               :value "small"}]}
   {:key     :value
    :type    :select
    :options [{:key   0
               :value "0"}
              {:key   10
               :value "10"}
              {:key   100
               :value "100"}
              {:key   1000
               :value "1000"}
              {:key   10000
               :value "10000"}]}
   {:key  :sufficient?
    :type :boolean}
   {:key  :purchasable?
    :type :boolean}
   {:key     :token
    :type    :select
    :options [{:key   "ETH"
               :value "ETH"}
              {:key   "SNT"
               :value "SNT"}]}])

(def eth-token (resources/get-token :eth))
(def snt-token (resources/get-token :snt))

(defn view
  []
  (let [state (reagent/atom {:size         :big
                             :value        10
                             :token        "ETH"
                             :sufficient?  false
                             :purchasable? false})]
    (fn []
      [preview/preview-container
       {:state      state
        :descriptor descriptor}
       [rn/view {:style {:align-items :center}}
        [quo/token-tag
         (assoc @state
                :img-src
                (if (= (get-in @state [:token]) "ETH") eth-token snt-token))]]])))
