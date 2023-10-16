(ns status-im2.subs.wallet-2.networks
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [quo2.foundations.resources :as resources]))

(re-frame/reg-sub
 :wallet-2/filtered-networks-by-mode
 :<- [:wallet-2/networks]
 (fn [[networks] [_ test?]]
   (let [filter-fn (if test? :Test :Prod)]
     (map filter-fn
          networks))))

(def default-network-details
  {:chain-id     1
   :source       (resources/networks :ethereum)
   :short-name   "eth"
   :network-name :ethereum})

(re-frame/reg-sub
 :wallet-2/network-details
 :<- [:wallet-2/filtered-networks-by-mode false]
 (fn [networks]
   (map
    (fn [{:keys [chainId shortName chainName]}]
      (let [network-name (keyword (string/lower-case chainName))]
        (if (= 1 chainId)
          default-network-details
          {:short-name   (str shortName (when (= network-name :arbitrum) "1"))
           :network-name network-name
           :source       (resources/networks network-name)
           :chain-id     chainId})))
    networks)))
