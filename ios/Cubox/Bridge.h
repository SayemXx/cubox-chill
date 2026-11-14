#ifndef CUBOX_BRIDGE_H
#define CUBOX_BRIDGE_H
char *cubox_request(const char *input, const char *path);
void cubox_free(char *value);
#endif
