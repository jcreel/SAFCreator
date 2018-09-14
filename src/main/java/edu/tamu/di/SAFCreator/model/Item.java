package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.Util;

public class Item {
    private Batch batch;
    private List<SchematicFieldSet> schemata;
    private List<Bundle> bundles;
    private String handle;

    private boolean cancelled;

    private File itemDirectory;

    public Item(int row, Batch batch) {
        this.batch = batch;
        schemata = new ArrayList<SchematicFieldSet>();
        bundles = new ArrayList<Bundle>();

        itemDirectory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + row);
        itemDirectory.mkdir();

        handle = null;
        cancelled = false;
    }

    private boolean checkIsCancelled(Object object, Method method) {
        try {
            Object arglist[] = new Object[0];
            Object result = method.invoke(object, arglist);
            Boolean isCancelled = (Boolean) result;
            cancelled = isCancelled.booleanValue();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return cancelled;
    }

    public Batch getBatch() {
        return batch;
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    public String getHandle() {
        return handle;
    }

    public Bundle getOrCreateBundle(String bundleName) {
        for (Bundle bundle : bundles) {
            if (bundle.getName().equals(bundleName)) {
                return bundle;
            }
        }

        Bundle bundle = new Bundle();
        bundle.setName(bundleName);
        bundle.setItem(this);
        bundles.add(bundle);
        return bundle;
    }

    public SchematicFieldSet getOrCreateSchema(String schemaName) {
        for (SchematicFieldSet schema : schemata) {
            if (schema.getSchemaName().equals(schemaName)) {
                return schema;
            }
        }

        SchematicFieldSet schema = new SchematicFieldSet();
        schema.setSchemaName(schemaName);
        schemata.add(schema);
        return schema;
    }

    public String getSAFDirectory() {
        return itemDirectory.getAbsolutePath();
    }

    public List<SchematicFieldSet> getSchemata() {
        return schemata;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    private void writeContents(List<Problem> problems, Object object, Method method) {
        String contentsString = "";
        for (Bundle bundle : bundles) {
            if (checkIsCancelled(object, method)) {
                return;
            }

            for (Bitstream bitstream : bundle.getBitstreams()) {
                if (checkIsCancelled(object, method)) {
                    return;
                }

                if (!batch.getIgnoreFiles()) {
                    bitstream.setAction(batch.getAction());
                    bitstream.copyMe(problems);
                }
                contentsString += bitstream.getContentsManifestLine();
            }
        }

        if (checkIsCancelled(object, method)) {
            return;
        }

        if (batch.getLicense() != null) {
            contentsString += batch.getLicense().getContentsManifestLine();
            batch.getLicense().writeToItem(this);
        }

        if (checkIsCancelled(object, method)) {
            return;
        }

        File contentsFile = new File(getSAFDirectory() + "/contents");
        try {
            if (!contentsFile.exists()) {
                contentsFile.createNewFile();
            }
            Util.setFileContents(contentsFile, contentsString);
        } catch (FileNotFoundException e) {
            Problem problem = new Problem(true, "Unable to write to missing contents file for item directory "
                    + getSAFDirectory() + ", reason: " + e.getMessage());
            problems.add(problem);
        } catch (IOException e) {
            Problem problem = new Problem(true, "Error writing contents file for item directory " + getSAFDirectory()
                    + ", reason: " + e.getMessage());
            problems.add(problem);
        }
    }

    private void writeHandle(List<Problem> problems, Object object, Method method) {
        File handleFile = new File(itemDirectory.getAbsolutePath() + "/handle");
        try {
            if (!handleFile.exists()) {
                handleFile.createNewFile();
            }
            Util.setFileContents(handleFile, getHandle());
        } catch (FileNotFoundException e) {
            Problem problem = new Problem(true, "Unable to write to missing handle file for item directory "
                    + getSAFDirectory() + ", reason: " + e.getMessage());
            problems.add(problem);
        } catch (IOException e) {
            Problem problem = new Problem(true, "Error writing handle file for item directory " + getSAFDirectory()
                    + ", reason: " + e.getMessage());
            problems.add(problem);
        }
    }

    public List<Problem> writeItemSAF(Object object, Method method) {
        List<Problem> problems = new ArrayList<Problem>();

        cancelled = false;

        if (!cancelled) {
            writeContents(problems, object, method);
        }
        if (!cancelled) {
            writeMetadata(problems, object, method);
        }
        if (!cancelled && getHandle() != null) {
            writeHandle(problems, object, method);
        }

        return problems;
    }

    private void writeMetadata(List<Problem> problems, Object object, Method method) {
        for (SchematicFieldSet schema : schemata) {
            File metadataFile = new File(itemDirectory.getAbsolutePath() + "/" + schema.getFilename());

            try {
                if (!metadataFile.exists()) {
                    metadataFile.createNewFile();
                }
                Util.setFileContents(metadataFile, schema.getXML());
            } catch (FileNotFoundException e) {
                Problem problem = new Problem(true, "Unable to write to missing metadata file "
                        + metadataFile.getAbsolutePath() + ", reason: " + e.getMessage());
                problems.add(problem);
            } catch (IOException e) {
                Problem problem = new Problem(true, "Unable to create metadata file " + metadataFile.getAbsolutePath()
                        + ", reason: " + e.getMessage());
                problems.add(problem);
            }
        }
    }
}
