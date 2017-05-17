package cz.studenthub.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class PagingUtil {

    public static <T> List<T> paging(ArrayList<T> list, int start, int size) {
        int listSize = list.size();
        if (listSize == 0)
            return list;

        if (start >= listSize)
            throw new WebApplicationException(Status.BAD_REQUEST);

        int remaining = listSize - start;

        if (size > remaining || size == 0)
            size = remaining;

        return list.subList(start, start + size);
    }

    public static <T> List<T> paging(List<T> list, int start, int size) {
        return paging(new ArrayList<T>(list), start, size);
    }

    public static <T> List<T> paging(Set<T> set, int start, int size) {
        return paging(new ArrayList<T>(set), start, size); 
    }
}