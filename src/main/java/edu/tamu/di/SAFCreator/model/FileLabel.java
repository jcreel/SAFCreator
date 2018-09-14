package edu.tamu.di.SAFCreator.model;

public class FileLabel extends CellDatumImpl implements ColumnLabel {
	private String bundleName;

	public FileLabel(String bundleName) {
		setBundleName(bundleName);
	}

	public String getBundleName() {
		return bundleName;
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean isHandle() {
		return true;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}
}
