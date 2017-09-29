//module com.aitusoftware.transport {
//}

/*
PageCache layout - an append-only paged data store

contains Records

each Record is stored at a position (long) within the PageCache

PageCache is split into Pages

each Record is stored at a pageOffset (int) within the Page

position = pageNumber * pageSize + pageOffset
pageOffset = position & (pageSize - 1)
See Offsets

 */