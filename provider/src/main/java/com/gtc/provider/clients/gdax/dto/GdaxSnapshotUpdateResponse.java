package com.gtc.provider.clients.gdax.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by mikro on 07.01.2018.
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class GdaxSnapshotUpdateResponse extends GdaxResponse {

    private String[][] changes;

}
