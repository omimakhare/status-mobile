(ns status-im2.contexts.wallet.common.utils
  (:require [status-im2.constants :as constants]
            [clojure.string :as string]))

(defn get-first-name
  [full-name]
  (first (string/split full-name #" ")))

(defn get-derivation-path
  [number-of-accounts]
  (str constants/path-wallet-root "/" number-of-accounts))
