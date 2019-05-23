package com.dawproject.springboot.beer.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dawproject.springboot.beer.models.entity.Cliente;
import com.dawproject.springboot.beer.models.service.IClienteService;
import com.dawproject.springboot.beer.models.service.IUploadFileService;
import com.dawproject.springboot.beer.util.paginator.PageRender;

@Controller
@SessionAttributes("cliente")
public class ClienteController {

	protected final Log logger = LogFactory.getLog(this.getClass());

	@Autowired
	private IClienteService clienteService;

	@Autowired
	private IUploadFileService UploadFileService;

//	private final Logger log = LoggerFactory.getLogger(getClass()); // Para hacer debug de los sitios de directorio
//	private final static String UPLOADS_FOLDER = "uploads";

	@Secured("ROLE_USER")
	@GetMapping(value = "/uploads/{filename:.+}")
	public ResponseEntity<Resource> verFoto(@PathVariable String filename) {

		Resource recurso = null;

		try {
			recurso = UploadFileService.load(filename);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"")
				.body(recurso);
	}

//	@Secured("ROLE_USER")
	@PreAuthorize("hasRole('ROLE_USER')")
	@GetMapping(value = "/ver/{id}")
	public String ver(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Cliente cliente = clienteService.fetchByIdWithFacturas(id); // clienteService.findOne(id);
		if (cliente == null) {
			flash.addFlashAttribute("error", "El cliente no existe en la base de datos");
			return "redirect:/listar";
		}

		model.put("cliente", cliente);
		model.put("titulo", "Detalles del cliente " + cliente.getNombre());

		return "ver";

	}

	@RequestMapping(value ="/listar", method = RequestMethod.GET)
	public String listar(@RequestParam(name = "page", defaultValue = "0") int page, Model model,
			Authentication authentication,
			HttpServletRequest request) {

		if (authentication != null) {
			logger.info("Hola usuario , tu username es:".concat(authentication.getName()));
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth != null) {
			logger.info(
					"Utilizando forma estatica SecurityContextHolder.getContext().getAuthentication() -> Hola usuario , tu username es:"
							.concat(auth.getName()));
		}

		if (hasRole("ROLE_ADMIN")) {
			logger.info("Hola ".concat(auth.getName()).concat(" tienes acceso"));
		} else {
			logger.info("Hola ".concat(auth.getName()).concat(" NO tienes acceso"));
		}
		
		SecurityContextHolderAwareRequestWrapper securityContext = new SecurityContextHolderAwareRequestWrapper(request, "");
		
		if(securityContext.isUserInRole("ROLE_ADMIN")) {
			logger.info("Usando SecurityContextHolderAwareRequestWrapper -> Hola: ".concat(auth.getName()).concat(" tienes acceso"));
		}else {
			logger.info("Usando SecurityContextHolderAwareRequestWrapper -> Hola: ".concat(auth.getName()).concat(" NO tienes acceso"));
		}
		
		if(request.isUserInRole("ROLE_ADMIN")) {
			logger.info("Usando HttpServletRequest -> Hola: ".concat(auth.getName()).concat(" tienes acceso"));
		}else {
			logger.info("Usando HttpServletRequest -> Hola: ".concat(auth.getName()).concat(" NO tienes acceso"));
		}

		Pageable pageRequest = PageRequest.of(page, 4);

		Page<Cliente> clientes = clienteService.findAll(pageRequest);

		PageRender<Cliente> pageRender = new PageRender<Cliente>("/listar", clientes);
		model.addAttribute("titulo", "Listado de clientes");
		model.addAttribute("clientes", clientes);
		model.addAttribute("page", pageRender);
		return "listar";

	}
	
	@Secured("ROLE_ADMIN")
	@RequestMapping(value = "/form")
	public String crear(Map<String, Object> model) {

		Cliente cliente = new Cliente();
		model.put("cliente", cliente);
		model.put("titulo", "Formulario de Cliente");
		return "form";
	}

	@RequestMapping(value = "/form/{id}")
	public String editar(@PathVariable(value = "id") Long id, Map<String, Object> model, RedirectAttributes flash) {

		Cliente cliente = null;

		if (id > 0) {
			cliente = clienteService.findOne(id);
			if (cliente == null) {
				flash.addFlashAttribute("error", "El id del cliente no existe en la base de datos");
				return "redirect:/listar";
			}
		} else {
			flash.addFlashAttribute("error", "El id del cliente no puede ser cero");
			return "redirect:/listar";
		}
		model.put("cliente", cliente);
		model.put("titulo", "Editar Cliente");
		return "form";
	}

	@Secured("ROLE_ADMIN")
	@RequestMapping(value = "/form", method = RequestMethod.POST)
	public String guardar(@Valid Cliente cliente, BindingResult result, Model model,
			@RequestParam("file") MultipartFile foto, RedirectAttributes flash, SessionStatus status) {

		if (result.hasErrors()) {
			model.addAttribute("titulo", "Formulario de Cliente");
			return "form";
		}

		if (!foto.isEmpty()) {

			if (cliente.getId() != null && cliente.getId() > 0 && cliente.getFoto() != null
					&& cliente.getFoto().length() > 0) {

				UploadFileService.delete(cliente.getFoto());
			}

			String uniqueFilename = null;
			try {
				uniqueFilename = UploadFileService.copy(foto);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

//			Path  directorioRecursos = Paths.get("src//main//resources//static//upload"); Ubicacion de guardado de imagenes en ruta local del proyecto
//			String rootPath = directorioRecursos.toFile().getAbsolutePath();

//			String rootPath = "C://Temp//uploads"; // Ruta externa al proyecto

			flash.addFlashAttribute("info", "Has subido correctamente '" + foto.getOriginalFilename() + "'");
//			flash.addFlashAttribute("info" , "Has subido correctamente '" + rootAbsolutePath + "'"); // Para comprobar que efectivamente el archivo se esta subiendo con el UUID
			cliente.setFoto(uniqueFilename);

		}
		String mensajeFlash = (cliente.getId() != null) ? "Cliente editado con exito" : "Cliente creado con exito";

		clienteService.save(cliente);
		status.setComplete();
		flash.addFlashAttribute("success", mensajeFlash);
		return "redirect:listar";

	}
	@Secured("ROLE_ADMIN")
	@RequestMapping(value = "/eliminar/{id}")
	public String eliminar(@PathVariable(value = "id") Long id, RedirectAttributes flash) {
		if (id > 0) {
			Cliente cliente = clienteService.findOne(id);

			clienteService.delete(id);
			flash.addFlashAttribute("success", "Cliente eliminado con exito!");

			if (UploadFileService.delete(cliente.getFoto())) {
				flash.addFlashAttribute("info", "Foto" + cliente.getFoto() + "eliminada con exito!");
			}
		}

		return "redirect:/listar";
	}

	private boolean hasRole(String role) {

		SecurityContext context = SecurityContextHolder.getContext();

		if (context == null) {
			return false;
		}
		Authentication auth = context.getAuthentication();

		if (auth == null) {
			return false;
		}

		Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

		return authorities.contains(new SimpleGrantedAuthority(role));
		
//		for (GrantedAuthority authority : authorities) {
//			if (role.equals(authority.getAuthority())) {
//				logger.info("Usuario: ".concat(auth.getName()).concat(" Rol: ".concat(authority.getAuthority())));
//				return true;
//			}
//		}
//
//		return false;

	}

}
