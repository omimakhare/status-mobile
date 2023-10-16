(ns status-im2.contexts.wallet.events
  (:require [utils.re-frame :as rf]
            [taoensso.timbre :as log]))

(rf/defn add-chains-to-db
  {:events [:wallet-2/add-chains-to-db]}
  [{:keys [db] :as cofx}]
  {:fx [[:json-rpc/call
         [{:method     "wallet_getEthereumChains"
           :params     []
           :on-success #(rf/dispatch [:wallet-2/add-chains-to-db %])
           :on-error   #(log/info "failed to get networks " %)}]]]})

(rf/reg-event-fx :wallet-2/add-chains-to-db
 (fn [{:keys [db]} data]
   {:db (assoc-in db [:wallet-2/networks] data)}))

