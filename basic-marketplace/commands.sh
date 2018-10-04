docker stop $(docker ps -aq)

docker rm $(docker ps -aq)

docker-compose -f docker-compose.yml up -d

# wait for Hyperledger Fabric to start
# incase of errors when running later commands, issue export FABRIC_START_TIMEOUT=<larger number>
export FABRIC_START_TIMEOUT=30
#echo ${FABRIC_START_TIMEOUT}
sleep ${FABRIC_START_TIMEOUT}

docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" cli peer channel create -o orderer.market.com:7050 -c channel -f /etc/hyperledger/configtx/channel.tx

docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Seller.market.com:7051" -e "CORE_PEER_ID=cli" cli peer channel join -b channel.block

docker exec -e "CORE_PEER_LOCALMSPID=BuyerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Buyer.market.com/users/Admin@Buyer.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Buyer.market.com:7051" -e "CORE_PEER_ID=cli" cli peer channel join -b channel.block

docker exec -e "CORE_PEER_LOCALMSPID=LogisticMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Logistic.market.com/users/Admin@Logistic.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Logistic.market.com:7051" -e "CORE_PEER_ID=cli" cli peer channel join -b channel.block

docker exec -e "CORE_PEER_LOCALMSPID=MarketplaceMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Marketplace.market.com/users/Admin@Marketplace.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Marketplace.market.com:7051" -e "CORE_PEER_ID=cli" cli peer channel join -b channel.block


sleep 3

docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Seller.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode install -n market-network -v 1.0 -l golang -p github.com/basic-marketplace-cc/go

docker exec -e "CORE_PEER_LOCALMSPID=BuyerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Buyer.market.com/users/Admin@Buyer.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Buyer.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode install -n market-network -v 1.0 -l golang -p github.com/basic-marketplace-cc/go

docker exec -e "CORE_PEER_LOCALMSPID=LogisticMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Logistic.market.com/users/Admin@Logistic.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Logistic.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode install -n market-network -v 1.0 -l golang -p github.com/basic-marketplace-cc/go

docker exec -e "CORE_PEER_LOCALMSPID=MarketplaceMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Marketplace.market.com/users/Admin@Marketplace.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Marketplace.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode install -n market-network -v 1.0 -l golang -p github.com/basic-marketplace-cc/go

sleep 3


docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Seller.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode instantiate -o orderer.market.com:7050 -v 1.0 -C channel -n market-network -l golang -v 1.0 -c '{"Args":[""]}' -P "OR ('SellerMSP.member', 'BuyerMSP.member', 'MarketplaceMSP.member', 'LogisticMSP.member')"

docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Seller.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode invoke -o orderer.market.com:7050 -C channel -n market-network -c '{"function":"addItem","Args":["ITEM0", "10", "100", "Submitted"]}'

docker exec -e "CORE_PEER_LOCALMSPID=SellerMSP" -e "CORE_PEER_MSPCONFIGPATH=/opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/peerOrganizations/Seller.market.com/users/Admin@Seller.market.com/msp" -e "CORE_PEER_ADDRESS=peer0.Seller.market.com:7051" -e "CORE_PEER_ID=cli" cli peer chaincode invoke -o orderer.market.com:7050 -C channel -n market-network -c '{"function":"itemList","Args":[""]}'
