package com.template.flows;

import com.template.states.IOUState;
import com.template.contracts.IOUContract;
import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.transactions.SignedTransaction;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;


import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class IOUFlow extends FlowLogic<Void> {
    private final Integer iouValue;
    private final Party otherParty;

    // We will not use these ProgressTracker for this Hello-World sample
    private final ProgressTracker progressTracker = new ProgressTracker();

    public IOUFlow(Integer iouValue, Party otherParty) {
        this.iouValue = iouValue;
        this.otherParty = otherParty;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // You retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

// You create the transaction components.
        IOUState outputState = new IOUState(iouValue, getOurIdentity(), otherParty);
        List<PublicKey> requiredSigners = Arrays.asList(getOurIdentity().getOwningKey(), otherParty.getOwningKey());
        Command command = new Command<>(new IOUContract.Create(), requiredSigners);

// You create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(outputState, IOUContract.ID)
                .addCommand(command);

// Verifying the transaction.
        txBuilder.verify(getServiceHub());

// Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

// Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

// Obtaining the counterparty's signature.
        SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(
                signedTx, Arrays.asList(otherPartySession), CollectSignaturesFlow.tracker()));

// Finalising the transaction.
        subFlow(new FinalityFlow(fullySignedTx, otherPartySession));

        return null;
    }
}
