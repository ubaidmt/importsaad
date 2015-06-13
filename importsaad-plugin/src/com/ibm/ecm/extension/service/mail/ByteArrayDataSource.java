package com.ibm.ecm.extension.service.mail;

import java.io.*;
import org.apache.commons.io.IOUtils;
import javax.activation.DataSource;

public class ByteArrayDataSource
    implements DataSource
{

    public ByteArrayDataSource(InputStream is, String type)
    {
        this.type = type;
        try
        {
        	data = IOUtils.toByteArray(is);
        }
        catch(IOException ioexception) { }
    }

    public ByteArrayDataSource(byte data[], String type)
    {
        this.data = data;
        this.type = type;
    }

    public ByteArrayDataSource(String data, String type)
    {
        try
        {
            this.data = data.getBytes("ISO-8859-1");
        }
        catch(UnsupportedEncodingException unsupportedencodingexception) { }
        this.type = type;
    }

    public InputStream getInputStream()
        throws IOException
    {
        if(data == null)
            throw new IOException("no data");
        else
            return new ByteArrayInputStream(data);
    }

    public OutputStream getOutputStream()
        throws IOException
    {
        throw new IOException("cannot do this");
    }

    public String getContentType()
    {
        return type;
    }

    public String getName()
    {
        return "dummy";
    }

    private byte data[];
    private String type;
}

