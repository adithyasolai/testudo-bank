package net.testudobank;

import lombok.Builder;
import lombok.Getter;

/**
 * Overdraft History Model
 */
@Builder
@Getter
public class OverdraftHistoryEntry {
    public final String time;
    public final String amount;
    public final String oldBalance;
    public final String newBalance;
}