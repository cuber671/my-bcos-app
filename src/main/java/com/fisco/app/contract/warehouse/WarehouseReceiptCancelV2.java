package com.fisco.app.contract.warehouse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple4;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple6;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

/**
 * 电子仓单作废合约 V2 支持作废原因存证、操作人记录及与后端状态机同步
 */
public class WarehouseReceiptCancelV2 extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b506110f3806100206000396000f3fe608060405234801561001057600080fd5b50600436106100575760003560e01c80630b036b6c1461005c578063354e2d601461008c57806379ddc1e8146100bf578063a3a042a1146100db578063fded9f1b14610110575b600080fd5b61007660048036038101906100719190610aec565b61012c565b6040516100839190610bac565b60405180910390f35b6100a660048036038101906100a19190610aec565b610162565b6040516100b69493929190610c68565b60405180910390f35b6100d960048036038101906100d49190610cc2565b6103be565b005b6100f560048036038101906100f09190610aec565b610611565b60405161010796959493929190610db4565b60405180910390f35b61012a60048036038101906101259190610e6a565b610890565b005b6000818051602081018201805184825260208301602085012081835280955050505050506000915054906101000a900460ff1681565b606080606060006001856040516101799190610f02565b908152602001604051809103902060050160009054906101000a900460ff166101d7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016101ce90610f65565b60405180910390fd5b60006001866040516101e99190610f02565b90815260200160405180910390209050806001018160020182600301836004015483805461021690610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461024290610fb4565b801561028f5780601f106102645761010080835404028352916020019161028f565b820191906000526020600020905b81548152906001019060200180831161027257829003601f168201915b505050505093508280546102a290610fb4565b80601f01602080910402602001604051908101604052809291908181526020018280546102ce90610fb4565b801561031b5780601f106102f05761010080835404028352916020019161031b565b820191906000526020600020905b8154815290600101906020018083116102fe57829003601f168201915b5050505050925081805461032e90610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461035a90610fb4565b80156103a75780601f1061037c576101008083540402835291602001916103a7565b820191906000526020600020905b81548152906001019060200180831161038a57829003601f168201915b505050505091509450945094509450509193509193565b600080856040516103cf9190610f02565b908152602001604051809103902060009054906101000a900460ff1690506002600e81111561040157610400610b35565b5b81600e81111561041457610413610b35565b5b148061044457506003600e81111561042f5761042e610b35565b5b81600e81111561044257610441610b35565b5b145b610483576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161047a90611058565b60405180910390fd5b600c6000866040516104959190610f02565b908152602001604051809103902060006101000a81548160ff0219169083600e8111156104c5576104c4610b35565b5b02179055506040518060c001604052808681526020018581526020018481526020018381526020014281526020016001151581525060018660405161050a9190610f02565b908152602001604051809103902060008201518160000190805190602001906105349291906108ef565b5060208201518160010190805190602001906105519291906108ef565b50604082015181600201908051906020019061056e9291906108ef565b50606082015181600301908051906020019061058b9291906108ef565b506080820151816004015560a08201518160050160006101000a81548160ff021916908315150217905550905050846040516105c79190610f02565b60405180910390207f812324bfbb5ccfd58260ed4809b8fa32f2e8f27970737078c25f76901265d70f84844260405161060293929190611078565b60405180910390a25050505050565b60018180516020810182018051848252602083016020850120818352809550505050505060009150905080600001805461064a90610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461067690610fb4565b80156106c35780601f10610698576101008083540402835291602001916106c3565b820191906000526020600020905b8154815290600101906020018083116106a657829003601f168201915b5050505050908060010180546106d890610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461070490610fb4565b80156107515780601f1061072657610100808354040283529160200191610751565b820191906000526020600020905b81548152906001019060200180831161073457829003601f168201915b50505050509080600201805461076690610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461079290610fb4565b80156107df5780601f106107b4576101008083540402835291602001916107df565b820191906000526020600020905b8154815290600101906020018083116107c257829003601f168201915b5050505050908060030180546107f490610fb4565b80601f016020809104026020016040519081016040528092919081815260200182805461082090610fb4565b801561086d5780601f106108425761010080835404028352916020019161086d565b820191906000526020600020905b81548152906001019060200180831161085057829003601f168201915b5050505050908060040154908060050160009054906101000a900460ff16905086565b8060ff16600e8111156108a6576108a5610b35565b5b6000836040516108b69190610f02565b908152602001604051809103902060006101000a81548160ff0219169083600e8111156108e6576108e5610b35565b5b02179055505050565b8280546108fb90610fb4565b90600052602060002090601f01602090048101928261091d5760008555610964565b82601f1061093657805160ff1916838001178555610964565b82800160010185558215610964579182015b82811115610963578251825591602001919060010190610948565b5b5090506109719190610975565b5090565b5b8082111561098e576000816000905550600101610976565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6109f9826109b0565b810181811067ffffffffffffffff82111715610a1857610a176109c1565b5b80604052505050565b6000610a2b610992565b9050610a3782826109f0565b919050565b600067ffffffffffffffff821115610a5757610a566109c1565b5b610a60826109b0565b9050602081019050919050565b82818337600083830152505050565b6000610a8f610a8a84610a3c565b610a21565b905082815260208101848484011115610aab57610aaa6109ab565b5b610ab6848285610a6d565b509392505050565b600082601f830112610ad357610ad26109a6565b5b8135610ae3848260208601610a7c565b91505092915050565b600060208284031215610b0257610b0161099c565b5b600082013567ffffffffffffffff811115610b2057610b1f6109a1565b5b610b2c84828501610abe565b91505092915050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602160045260246000fd5b600f8110610b7557610b74610b35565b5b50565b6000819050610b8682610b64565b919050565b6000610b9682610b78565b9050919050565b610ba681610b8b565b82525050565b6000602082019050610bc16000830184610b9d565b92915050565b600081519050919050565b600082825260208201905092915050565b60005b83811015610c01578082015181840152602081019050610be6565b83811115610c10576000848401525b50505050565b6000610c2182610bc7565b610c2b8185610bd2565b9350610c3b818560208601610be3565b610c44816109b0565b840191505092915050565b6000819050919050565b610c6281610c4f565b82525050565b60006080820190508181036000830152610c828187610c16565b90508181036020830152610c968186610c16565b90508181036040830152610caa8185610c16565b9050610cb96060830184610c59565b95945050505050565b60008060008060808587031215610cdc57610cdb61099c565b5b600085013567ffffffffffffffff811115610cfa57610cf96109a1565b5b610d0687828801610abe565b945050602085013567ffffffffffffffff811115610d2757610d266109a1565b5b610d3387828801610abe565b935050604085013567ffffffffffffffff811115610d5457610d536109a1565b5b610d6087828801610abe565b925050606085013567ffffffffffffffff811115610d8157610d806109a1565b5b610d8d87828801610abe565b91505092959194509250565b60008115159050919050565b610dae81610d99565b82525050565b600060c0820190508181036000830152610dce8189610c16565b90508181036020830152610de28188610c16565b90508181036040830152610df68187610c16565b90508181036060830152610e0a8186610c16565b9050610e196080830185610c59565b610e2660a0830184610da5565b979650505050505050565b600060ff82169050919050565b610e4781610e31565b8114610e5257600080fd5b50565b600081359050610e6481610e3e565b92915050565b60008060408385031215610e8157610e8061099c565b5b600083013567ffffffffffffffff811115610e9f57610e9e6109a1565b5b610eab85828601610abe565b9250506020610ebc85828601610e55565b9150509250929050565b600081905092915050565b6000610edc82610bc7565b610ee68185610ec6565b9350610ef6818560208601610be3565b80840191505092915050565b6000610f0e8284610ed1565b915081905092915050565b7f5265636f7264206e6f7420666f756e6400000000000000000000000000000000600082015250565b6000610f4f601083610bd2565b9150610f5a82610f19565b602082019050919050565b60006020820190508181036000830152610f7e81610f42565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b60006002820490506001821680610fcc57607f821691505b60208210811415610fe057610fdf610f85565b","5b50919050565b7f436f6e7472616374204572726f723a204f6e6c79204e4f524d414c206f72204660008201527f41494c4544207374617475732063616e2062652063616e63656c6c6564000000602082015250565b6000611042603d83610bd2565b915061104d82610fe6565b604082019050919050565b6000602082019050818103600083015261107181611035565b9050919050565b600060608201905081810360008301526110928186610c16565b905081810360208301526110a68185610c16565b90506110b56040830184610c59565b94935050505056fea2646970667358221220024974133fb3c7c8418c57499493095041edae5c68896ab2630bc21ed5f38d9364736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b506110f3806100206000396000f3fe608060405234801561001057600080fd5b50600436106100575760003560e01c8063032971151461005c578063829569571461008c57806393f5c98f146100c157806397468b02146100dd578063f815cb9a14610110575b600080fd5b61007660048036038101906100719190610aec565b61012c565b6040516100839190610bac565b60405180910390f35b6100a660048036038101906100a19190610aec565b610162565b6040516100b896959493929190610c83565b60405180910390f35b6100db60048036038101906100d69190610d00565b6103e1565b005b6100f760048036038101906100f29190610aec565b610634565b6040516101079493929190610dd7565b60405180910390f35b61012a60048036038101906101259190610e6a565b610890565b005b6000818051602081018201805184825260208301602085012081835280955050505050506000915054906101000a900460ff1681565b60018180516020810182018051848252602083016020850120818352809550505050505060009150905080600001805461019b90610ef5565b80601f01602080910402602001604051908101604052809291908181526020018280546101c790610ef5565b80156102145780601f106101e957610100808354040283529160200191610214565b820191906000526020600020905b8154815290600101906020018083116101f757829003601f168201915b50505050509080600101805461022990610ef5565b80601f016020809104026020016040519081016040528092919081815260200182805461025590610ef5565b80156102a25780601f10610277576101008083540402835291602001916102a2565b820191906000526020600020905b81548152906001019060200180831161028557829003601f168201915b5050505050908060020180546102b790610ef5565b80601f01602080910402602001604051908101604052809291908181526020018280546102e390610ef5565b80156103305780601f1061030557610100808354040283529160200191610330565b820191906000526020600020905b81548152906001019060200180831161031357829003601f168201915b50505050509080600301805461034590610ef5565b80601f016020809104026020016040519081016040528092919081815260200182805461037190610ef5565b80156103be5780601f10610393576101008083540402835291602001916103be565b820191906000526020600020905b8154815290600101906020018083116103a157829003601f168201915b5050505050908060040154908060050160009054906101000a900460ff16905086565b600080856040516103f29190610f63565b908152602001604051809103902060009054906101000a900460ff1690506002600e81111561042457610423610b35565b5b81600e81111561043757610436610b35565b5b148061046757506003600e81111561045257610451610b35565b5b81600e81111561046557610464610b35565b5b145b6104a6576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161049d90610fec565b60405180910390fd5b600c6000866040516104b89190610f63565b908152602001604051809103902060006101000a81548160ff0219169083600e8111156104e8576104e7610b35565b5b02179055506040518060c001604052808681526020018581526020018481526020018381526020014281526020016001151581525060018660405161052d9190610f63565b908152602001604051809103902060008201518160000190805190602001906105579291906108ef565b5060208201518160010190805190602001906105749291906108ef565b5060408201518160020190805190602001906105919291906108ef565b5060608201518160030190805190602001906105ae9291906108ef565b506080820151816004015560a08201518160050160006101000a81548160ff021916908315150217905550905050846040516105ea9190610f63565b60405180910390207f1e2270ce7ce25525524e946a1e399d2a98e42f515d45cd5c78136c237230bd818484426040516106259392919061100c565b60405180910390a25050505050565b6060806060600060018560405161064b9190610f63565b908152602001604051809103902060050160009054906101000a900460ff166106a9576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016106a09061109d565b60405180910390fd5b60006001866040516106bb9190610f63565b9081526020016040518091039020905080600101816002018260030183600401548380546106e890610ef5565b80601f016020809104026020016040519081016040528092919081815260200182805461071490610ef5565b80156107615780601f1061073657610100808354040283529160200191610761565b820191906000526020600020905b81548152906001019060200180831161074457829003601f168201915b5050505050935082805461077490610ef5565b80601f01602080910402602001604051908101604052809291908181526020018280546107a090610ef5565b80156107ed5780601f106107c2576101008083540402835291602001916107ed565b820191906000526020600020905b8154815290600101906020018083116107d057829003601f168201915b5050505050925081805461080090610ef5565b80601f016020809104026020016040519081016040528092919081815260200182805461082c90610ef5565b80156108795780601f1061084e57610100808354040283529160200191610879565b820191906000526020600020905b81548152906001019060200180831161085c57829003601f168201915b505050505091509450945094509450509193509193565b8060ff16600e8111156108a6576108a5610b35565b5b6000836040516108b69190610f63565b908152602001604051809103902060006101000a81548160ff0219169083600e8111156108e6576108e5610b35565b5b02179055505050565b8280546108fb90610ef5565b90600052602060002090601f01602090048101928261091d5760008555610964565b82601f1061093657805160ff1916838001178555610964565b82800160010185558215610964579182015b82811115610963578251825591602001919060010190610948565b5b5090506109719190610975565b5090565b5b8082111561098e576000816000905550600101610976565b5090565b6000604051905090565b600080fd5b600080fd5b600080fd5b600080fd5b6000601f19601f8301169050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052604160045260246000fd5b6109f9826109b0565b810181811067ffffffffffffffff82111715610a1857610a176109c1565b5b80604052505050565b6000610a2b610992565b9050610a3782826109f0565b919050565b600067ffffffffffffffff821115610a5757610a566109c1565b5b610a60826109b0565b9050602081019050919050565b82818337600083830152505050565b6000610a8f610a8a84610a3c565b610a21565b905082815260208101848484011115610aab57610aaa6109ab565b5b610ab6848285610a6d565b509392505050565b600082601f830112610ad357610ad26109a6565b5b8135610ae3848260208601610a7c565b91505092915050565b600060208284031215610b0257610b0161099c565b5b600082013567ffffffffffffffff811115610b2057610b1f6109a1565b5b610b2c84828501610abe565b91505092915050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052602160045260246000fd5b600f8110610b7557610b74610b35565b5b50565b6000819050610b8682610b64565b919050565b6000610b9682610b78565b9050919050565b610ba681610b8b565b82525050565b6000602082019050610bc16000830184610b9d565b92915050565b600081519050919050565b600082825260208201905092915050565b60005b83811015610c01578082015181840152602081019050610be6565b83811115610c10576000848401525b50505050565b6000610c2182610bc7565b610c2b8185610bd2565b9350610c3b818560208601610be3565b610c44816109b0565b840191505092915050565b6000819050919050565b610c6281610c4f565b82525050565b60008115159050919050565b610c7d81610c68565b82525050565b600060c0820190508181036000830152610c9d8189610c16565b90508181036020830152610cb18188610c16565b90508181036040830152610cc58187610c16565b90508181036060830152610cd98186610c16565b9050610ce86080830185610c59565b610cf560a0830184610c74565b979650505050505050565b60008060008060808587031215610d1a57610d1961099c565b5b600085013567ffffffffffffffff811115610d3857610d376109a1565b5b610d4487828801610abe565b945050602085013567ffffffffffffffff811115610d6557610d646109a1565b5b610d7187828801610abe565b935050604085013567ffffffffffffffff811115610d9257610d916109a1565b5b610d9e87828801610abe565b925050606085013567ffffffffffffffff811115610dbf57610dbe6109a1565b5b610dcb87828801610abe565b91505092959194509250565b60006080820190508181036000830152610df18187610c16565b90508181036020830152610e058186610c16565b90508181036040830152610e198185610c16565b9050610e286060830184610c59565b95945050505050565b600060ff82169050919050565b610e4781610e31565b8114610e5257600080fd5b50565b600081359050610e6481610e3e565b92915050565b60008060408385031215610e8157610e8061099c565b5b600083013567ffffffffffffffff811115610e9f57610e9e6109a1565b5b610eab85828601610abe565b9250506020610ebc85828601610e55565b9150509250929050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b60006002820490506001821680610f0d57607f821691505b60208210811415610f2157610f20610ec6565b5b50919050565b600081905092915050565b6000610f3d82610bc7565b610f478185610f27565b9350610f57818560208601610be3565b80840191505092915050565b6000610f6f8284610f32565b915081905092915050565b7f436f6e7472616374204572726f723a204f6e6c79204e4f524d414c206f72204660008201527f41494c4544207374617475732063616e2062652063616e63656c6c6564000000602082015250565b6000610fd6603d83610bd2565b9150610fe182610f7a","565b604082019050919050565b6000602082019050818103600083015261100581610fc9565b9050919050565b600060608201905081810360008301526110268186610c16565b9050818103602083015261103a8185610c16565b90506110496040830184610c59565b949350505050565b7f5265636f7264206e6f7420666f756e6400000000000000000000000000000000600082015250565b6000611087601083610bd2565b915061109282611051565b602082019050919050565b600060208201905081810360008301526110b68161107a565b905091905056fea26469706673582212202ebb7bf2c57cdc661d7882c806bcd35c46a614847181a6a6bcadfe9a9f96b01264736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"cancelType\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"string\",\"name\":\"cancelledBy\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"ReceiptCancelled\",\"type\":\"event\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_receiptId\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_reason\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_cancelType\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"_operatorId\",\"type\":\"string\"}],\"name\":\"cancelReceipt\",\"outputs\":[],\"selector\":[2044576232,2482358671],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"name\":\"cancelRecords\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"cancelReason\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"cancelType\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"cancelledBy\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"cancelTime\",\"type\":\"uint256\"},{\"internalType\":\"bool\",\"name\":\"isExist\",\"type\":\"bool\"}],\"selector\":[2745189025,2190829911],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_receiptId\",\"type\":\"string\"}],\"name\":\"getCancelDetail\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"reason\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"cType\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"operator\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"time\",\"type\":\"uint256\"}],\"selector\":[894315872,2537982722],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"_receiptId\",\"type\":\"string\"},{\"internalType\":\"uint8\",\"name\":\"_status\",\"type\":\"uint8\"}],\"name\":\"initReceiptStatus\",\"outputs\":[],\"selector\":[4260208411,4162177946],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":0}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"}],\"name\":\"receiptStatuses\",\"outputs\":[{\"internalType\":\"enum WarehouseReceiptCancelV2.ReceiptStatus\",\"name\":\"\",\"type\":\"uint8\"}],\"selector\":[184773484,53047573],\"stateMutability\":\"view\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_CANCELRECEIPT = "cancelReceipt";

    public static final String FUNC_CANCELRECORDS = "cancelRecords";

    public static final String FUNC_GETCANCELDETAIL = "getCancelDetail";

    public static final String FUNC_INITRECEIPTSTATUS = "initReceiptStatus";

    public static final String FUNC_RECEIPTSTATUSES = "receiptStatuses";

    public static final Event RECEIPTCANCELLED_EVENT = new Event("ReceiptCancelled", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
    ;

    protected WarehouseReceiptCancelV2(String contractAddress, Client client,
            CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<ReceiptCancelledEventResponse> getReceiptCancelledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(RECEIPTCANCELLED_EVENT, transactionReceipt);
        ArrayList<ReceiptCancelledEventResponse> responses = new ArrayList<ReceiptCancelledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ReceiptCancelledEventResponse typedResponse = new ReceiptCancelledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.receiptId = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.cancelType = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.cancelledBy = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeReceiptCancelledEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(RECEIPTCANCELLED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeReceiptCancelledEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(RECEIPTCANCELLED_EVENT);
        subscribeEvent(topic0,callback);
    }

    /**
     * 执行仓单作废 
     * @param _cancelType 作废类型 
     * @param _operatorId 操作人ID (对应后端 userId) 
     * @param _reason 作废原因 
     * @param _receiptId 仓单ID 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt cancelReceipt(String _receiptId, String _reason, String _cancelType,
            String _operatorId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CANCELRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_reason), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_cancelType), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_operatorId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodCancelReceiptRawFunction(String _receiptId, String _reason,
            String _cancelType, String _operatorId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANCELRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_reason), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_cancelType), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_operatorId)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForCancelReceipt(String _receiptId, String _reason,
            String _cancelType, String _operatorId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CANCELRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_reason), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_cancelType), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_operatorId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 执行仓单作废 
     * @param _cancelType 作废类型 
     * @param _operatorId 操作人ID (对应后端 userId) 
     * @param _reason 作废原因 
     * @param _receiptId 仓单ID 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String cancelReceipt(String _receiptId, String _reason, String _cancelType,
            String _operatorId, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CANCELRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_reason), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_cancelType), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_operatorId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple4<String, String, String, String> getCancelReceiptInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANCELRECEIPT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple4<String, String, String, String>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (String) results.get(2).getValue(), 
                (String) results.get(3).getValue()
                );
    }

    public Tuple6<String, String, String, String, BigInteger, Boolean> cancelRecords(String param0)
            throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANCELRECORDS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return new Tuple6<String, String, String, String, BigInteger, Boolean>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (String) results.get(2).getValue(), 
                (String) results.get(3).getValue(), 
                (BigInteger) results.get(4).getValue(), 
                (Boolean) results.get(5).getValue()
                );
    }

    public Function getMethodCancelRecordsRawFunction(String param0) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANCELRECORDS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 查询作废详情（供后端对账使用） 
     */
    public Tuple4<String, String, String, BigInteger> getCancelDetail(String _receiptId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETCANCELDETAIL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return new Tuple4<String, String, String, BigInteger>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (String) results.get(2).getValue(), 
                (BigInteger) results.get(3).getValue()
                );
    }

    public Function getMethodGetCancelDetailRawFunction(String _receiptId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETCANCELDETAIL, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        return function;
    }

    /**
     * 模拟初始化仓单状态（实际应由创建合约调用或在此合约维护） 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt initReceiptStatus(String _receiptId, BigInteger _status) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INITRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_status)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodInitReceiptStatusRawFunction(String _receiptId, BigInteger _status)
            throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INITRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_status)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public String getSignedTransactionForInitReceiptStatus(String _receiptId, BigInteger _status) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INITRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_status)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 模拟初始化仓单状态（实际应由创建合约调用或在此合约维护） 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String initReceiptStatus(String _receiptId, BigInteger _status,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INITRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(_receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(_status)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, BigInteger> getInitReceiptStatusInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INITRECEIPTSTATUS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint8>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, BigInteger>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue()
                );
    }

    public BigInteger receiptStatuses(String param0) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_RECEIPTSTATUSES, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodReceiptStatusesRawFunction(String param0) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_RECEIPTSTATUSES, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return function;
    }

    public static WarehouseReceiptCancelV2 load(String contractAddress, Client client,
            CryptoKeyPair credential) {
        return new WarehouseReceiptCancelV2(contractAddress, client, credential);
    }

    public static WarehouseReceiptCancelV2 deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        return deploy(WarehouseReceiptCancelV2.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
    }

    public static class ReceiptCancelledEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] receiptId;

        public String cancelType;

        public String cancelledBy;

        public BigInteger timestamp;
    }
}
