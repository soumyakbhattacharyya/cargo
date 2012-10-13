/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.daemon.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.codehaus.cargo.util.DefaultFileHandler;
import org.codehaus.cargo.util.FileHandler;

/**
 * File manager to deal with files and directories in the daemon workspace.
 *
 * @version $Id$
 */
public class FileManager
{
    /**
     * The cargo home directory.
     */
    private String cargoHomeDirectory;

    /**
     * The workspace directory.
     */
    private String workspaceDirectory;

    /**
     * The install directory.
     */
    private String installDirectory;

    /**
     * The configuration home directory.
     */
    private String configurationHomeDirectory;

    /**
     * The log directory.
     */
    private String logDirectory;

    /**
     * The file handler.
     */
    private final FileHandler fileHandler = new DefaultFileHandler();

    /**
     * Get the cargo home directory.
     *
     * @return the cargo home directory
     */
    public String getCargoHomeDirectory()
    {

        if (cargoHomeDirectory == null)
        {
            cargoHomeDirectory = System.getProperty("cargo.home");

            if (cargoHomeDirectory == null)
            {
                cargoHomeDirectory = fileHandler.getTmpPath(".");
            }
        }

        return cargoHomeDirectory;
    }

    /**
     * Get the workspace directory.
     *
     * @return the workspace directory
     */
    public String getWorkspaceDirectory()
    {
        if (workspaceDirectory == null)
        {
            workspaceDirectory = fileHandler.append(getCargoHomeDirectory(), "workspace");
        }

        return workspaceDirectory;
    }

    /**
     * Get the install directory.
     *
     * @return the install directory
     */
    public String getInstallDirectory()
    {
        if (installDirectory == null)
        {
            installDirectory = fileHandler.append(getCargoHomeDirectory(), "installs");
        }

        return installDirectory;
    }

    /**
     * Get the workspace directory for a container.
     *
     * @param handleId The handle identifier of a container
     * @return the workspace directory
     */
    public String getWorkspaceDirectory(String handleId)
    {
        if (workspaceDirectory == null)
        {
            workspaceDirectory = fileHandler.append(getCargoHomeDirectory(), "workspace");
        }

        return fileHandler.append(workspaceDirectory, handleId);
    }

    /**
     * Get the configuration home directory.
     *
     * @return the configuration home directory
     */
    public String getConfigurationDirectory()
    {
        if (configurationHomeDirectory == null)
        {
            configurationHomeDirectory =
                fileHandler.append(getCargoHomeDirectory(), "configurations");
        }

        return configurationHomeDirectory;
    }

    /**
     * Get the log directory.
     *
     * @return the log directory
     */
    public String getLogDirectory()
    {
        if (logDirectory == null)
        {
            logDirectory =
                fileHandler.append(getCargoHomeDirectory(), "logs");
        }

        return logDirectory;
    }

    /**
     * Get the log directory for a container.
     *
     * @param handleId The handle identifier of a container
     * @return the log directory for a container
     */
    public String getLogDirectory(String handleId)
    {
        return fileHandler.append(getLogDirectory(), handleId);
    }

    /**
     * Get the log file for a container.
     *
     * @param handleId The handle identifier of a container
     * @param filename The log file name
     * @return the log directory for a container
     */
    public String getLogFile(String handleId, String filename)
    {
        File file = new File(filename);

        if (file.isAbsolute())
        {
            return filename;
        }
        else
        {
            return fileHandler.append(getLogDirectory(handleId), filename);
        }
    }

    /**
     * Get the configuration home directory for a container.
     *
     * @param handleId The handle identifier of a container
     * @return the default configuration home directory for a container
     */
    public String getConfigurationDirectory(String handleId)
    {
        return fileHandler.append(getConfigurationDirectory(), handleId);
    }

    /**
     * Delete temporary files.
     */
    public void deleteWorkspaceFiles()
    {
        fileHandler.delete(getWorkspaceDirectory());
    }

    /**
     * Saves the input stream to a file, relative to the workspace directory.
     *
     * @param relativeFile The relative filename
     * @param inputStream The inputstream containing the file contents
     * @return path to the saved file
     */
    public String saveFile(String relativeFile, InputStream inputStream)
    {
        String file = fileHandler.append(getWorkspaceDirectory(), relativeFile);

        fileHandler.copy(inputStream, fileHandler.getOutputStream(file));

        return file;
    }

    /**
     * Saves the input stream to a file, relative to the workspace directory of a container.
     *
     * @param handleId The handle identifier of a container
     * @param relativeFile The relative filename
     * @param inputStream The inputstream containing the file contents
     * @return path to the saved file
     */
    public String saveFile(String handleId, String relativeFile, InputStream inputStream)
    {
        String file = fileHandler.append(getWorkspaceDirectory(handleId), relativeFile);

        fileHandler.copy(inputStream, fileHandler.getOutputStream(file));

        return file;
    }

    /**
     * Saves the input stream to a file, relative to the workspace directory of a container
     * and a given directory.
     *
     * @param handleId The handle identifier of a container
     * @param relativeDirectory The relative directory
     * @param relativeFile The relative filename
     * @param inputStream The inputstream containing the file contents
     * @return path to the saved file
     */
    public String saveFile(String handleId, String relativeDirectory, String relativeFile,
        InputStream inputStream)
    {
        String file = fileHandler.append(fileHandler.append(getWorkspaceDirectory(handleId),
            relativeDirectory), relativeFile);

        fileHandler.copy(inputStream, fileHandler.getOutputStream(file));

        return file;
    }

    /**
     * Check if filename exists in the workspace.
     *
     * @param filename The file to check
     * @return true if file exists
     */
    public boolean existsFile(String filename)
    {
        String filepath = fileHandler.append(getWorkspaceDirectory(), filename);
        return fileHandler.exists(filepath);
    }

    /**
     * Get the URL for a filename in the workspace.
     *
     * @param filename The filename to construct URL for
     * @return the URL for the filename
     */
    public String getFileURL(String filename)
    {
        String filepath = fileHandler.append(getWorkspaceDirectory(), filename);
        return fileHandler.getURL(filepath);
    }

    /**
     * Get the file input stream
     *
     * @param filename The filename to get input stream from
     * @return the input stream
     */
    public InputStream getFileInputStream(String filename)
    {
        return fileHandler.getInputStream(filename);
    }

    /**
     * Copies the given file to the output stream
     *
     * @param filename The file to copy
     * @param out The destination output stream
     * @throws IOException if error happens
     */
    public void copy(String filename, OutputStream out) throws IOException
    {
        BufferedInputStream is = new BufferedInputStream(getFileInputStream(filename));
        byte[] buf = new byte[64 * 1024];
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1)
        {
            out.write(buf, 0, bytesRead);
        }
        is.close();
        out.flush();
        out.close();
    }

}