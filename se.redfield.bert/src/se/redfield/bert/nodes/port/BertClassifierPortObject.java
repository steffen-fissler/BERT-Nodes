/*
 * Copyright (c) 2020 Redfield AB.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, Version 3, as
 * published by the Free Software Foundation.
 *  
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package se.redfield.bert.nodes.port;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.node.workflow.ModelContentOutPortView;

/**
 * Port object containing BERT classifier.
 * 
 * @author Alexander Bondaletov
 *
 */
public class BertClassifierPortObject extends FileStorePortObject {

	/**
	 * The type of this port.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(BertClassifierPortObject.class);

	private static final String KEY_MAX_SEQ_LENGTH = "maxSeqLength";

	private BertClassifierPortObjectSpec spec;
	private int maxSeqLength;

	/**
	 * Creates new instance
	 * 
	 * @param spec         the spec
	 * @param fileStore    the file store
	 * @param maxSeqLength the max sequence length
	 */
	public BertClassifierPortObject(BertClassifierPortObjectSpec spec, FileStore fileStore, int maxSeqLength) {
		super(Arrays.asList(fileStore));
		this.spec = spec;
		this.maxSeqLength = maxSeqLength;
	}

	/**
	 * Creates new instance
	 * 
	 */
	public BertClassifierPortObject() {
		super();
	}

	@Override
	public String getSummary() {
		return spec.toString();
	}

	@Override
	public PortObjectSpec getSpec() {
		return spec;
	}

	@Override
	public JComponent[] getViews() {
		final ModelContent model = new ModelContent("Model Content");
		return new JComponent[] { new ModelContentOutPortView(model) };
	}

	/**
	 * @return the file store containing the model
	 */
	public FileStore getFileStore() {
		return getFileStore(0);
	}

	/**
	 * @return the max sequence length
	 */
	public int getMaxSeqLength() {
		return maxSeqLength;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BertClassifierPortObject)) {
			return false;
		}
		BertClassifierPortObject other = (BertClassifierPortObject) obj;

		return maxSeqLength == other.maxSeqLength && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(maxSeqLength) + super.hashCode();
	}

	protected void load(ModelContentRO model, BertClassifierPortObjectSpec spec) throws InvalidSettingsException {
		this.spec = spec;
		this.maxSeqLength = model.getInt(KEY_MAX_SEQ_LENGTH);
	}

	protected void save(ModelContentWO model) {
		model.addInt(KEY_MAX_SEQ_LENGTH, maxSeqLength);
	}

	/**
	 * Serializer for {@link BertClassifierPortObject}
	 */
	public static final class Serializer extends PortObjectSerializer<BertClassifierPortObject> {

		@Override
		public void savePortObject(final BertClassifierPortObject portObject, final PortObjectZipOutputStream out,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			ModelContent model = new ModelContent("model.xml");
			model.addInt("version", 1);
			ModelContentWO subModel = model.addModelContent("model");
			portObject.save(subModel);
			out.putNextEntry(new ZipEntry("content.xml"));
			model.saveToXML(out);

		}

		@Override
		public BertClassifierPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			ZipEntry entry = in.getNextEntry();
			if (!"content.xml".equals(entry.getName())) {
				throw new IOException("Expected zip entry content.xml, got " + entry.getName());
			}
			ModelContentRO model = ModelContent.loadFromXML(new NonClosableInputStream.Zip(in));

			try {
				BertClassifierPortObject obj = new BertClassifierPortObject();
				obj.load(model.getModelContent("model"), (BertClassifierPortObjectSpec) spec);
				return obj;
			} catch (InvalidSettingsException e) {
				throw new IOException("Unable to load model content:" + e.getMessage(), e);
			}
		}

	}

}
