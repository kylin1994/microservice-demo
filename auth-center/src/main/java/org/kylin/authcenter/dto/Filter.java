package org.kylin.authcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kylin.authcenter.constant.Operation;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Filter implements Serializable {

    private static final long serialVersionUID = -1216382529193961553L;
    String key;
    Object value;
    Operation op;
}
