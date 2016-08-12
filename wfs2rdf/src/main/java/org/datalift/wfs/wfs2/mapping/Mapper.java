package org.datalift.wfs.wfs2.mapping;

import org.datalift.geoutility.Context;
import org.datalift.model.ComplexFeature;

public interface Mapper {
	public void map(ComplexFeature cf, Context ctx);
}
