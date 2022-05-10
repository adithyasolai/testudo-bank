package net.testudobank;

import lombok.Builder;
import lombok.Getter;

/**
 * Transfer History Model
 */
@Builder
@Getter
public class TransferHistoryEntry {
    public String time;
    public String amount;
    public String to;
    public String from;
}
