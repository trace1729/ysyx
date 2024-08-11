ENTRY(_start)

MEMORY {
	MROM (rx): ORIGIN = 0x20000000, LENGTH = 0x1000
	SRAM (rwx): ORIGIN = 0x0f000000, LENGTH = 0x2000
}

SECTIONS {
  .text : 
	{
    *(entry)
    *(.text*)
    *(.rodata*)
		*(.srodata*)
		*(.rela*)
		_etext = . ;
  }>MROM
	
  .data : 
	AT( LOADADDR(.text) + SIZEOF ( .text ) ) 
	{
		_sdata = . ;
		*(.sdata*)
    *(.data*)
		_edata = . ;
  } >SRAM

  .bss (NOLOAD) :
	AT( LOADADDR(.data) + SIZEOF(.data))
	{
		_bss_start = . ;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
		_bss_end = . ;
  } >SRAM
	.stack (NOLOAD):
	{
		. = _bss_end;
		. = ALIGN(0x100);
		_stack_top = . ;
		_stack_pointer = . + 0x800;
	}
	_heap_start = _stack_pointer;
	_heap_end = ORIGIN(SRAM) + LENGTH(SRAM);
	end = . ;
	_end = . ;
}
